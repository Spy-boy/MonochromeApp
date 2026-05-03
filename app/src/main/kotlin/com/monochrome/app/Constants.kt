package com.monochrome.app

object Constants {
    const val SITE_URL = "https://monochrome.tf/"

    // ─── Intent Actions ──────────────────────────────────────────────────
    const val ACTION_PLAY          = "com.monochrome.app.PLAY"
    const val ACTION_PAUSE         = "com.monochrome.app.PAUSE"
    const val ACTION_NEXT          = "com.monochrome.app.NEXT"
    const val ACTION_PREVIOUS      = "com.monochrome.app.PREVIOUS"
    const val ACTION_UPDATE_STATE  = "com.monochrome.app.UPDATE_STATE"

    // ─── Intent Extras ───────────────────────────────────────────────────
    const val EXTRA_IS_PLAYING     = "is_playing"

    // ─── MIME Types ──────────────────────────────────────────────────────
    const val MIME_MPEG            = "audio/mpeg"
    const val MIME_FLAC            = "audio/flac"
    const val MIME_OCTET_STREAM    = "application/octet-stream"
    const val MIME_HTML            = "text/html"
    const val MIME_JS              = "application/javascript"
    const val MIME_CSS             = "text/css"

    // ─── Network Constants ───────────────────────────────────────────────
    const val DEFAULT_ORIGIN       = "https://monochrome.tf"
    const val PROXY_UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"

    const val JS_HOOKS = """
(function(){
  // 1. MediaSession Polyfill & Interceptor
  try {
    var ms = navigator.mediaSession;
    if (!ms || typeof ms.setActionHandler !== 'function') {
      var stub = {
        metadata: null, playbackState: 'none',
        setActionHandler: function(){}, setPositionState: function(){},
        setMicrophoneActive: function(){}, setCameraActive: function(){}
      };
      Object.defineProperty(navigator, 'mediaSession', { get: function(){ return stub; }, configurable: true });
    }
    
    var origMs = navigator.mediaSession;
    if(!window.__mcHandlers) window.__mcHandlers = {};
    
    // Wrap setActionHandler to capture site callbacks
    var _origSAH = origMs.setActionHandler.bind(origMs);
    origMs.setActionHandler = function(action, handler){
      window.__mcHandlers[action] = handler;
      try { return _origSAH(action, handler); } catch(e){}
    };

    // Wrap metadata setter to catch updates
    var _metadata = origMs.metadata;
    Object.defineProperty(origMs, 'metadata', {
      get: function() { return _metadata; },
      set: function(value) {
        _metadata = value;
        if (value) {
           var art = (value.artwork && value.artwork.length > 0) ? value.artwork[0].src : '';
           MonochromeApp.onMetadataChanged(value.title || '', value.artist || '', art);
        }
      },
      configurable: true
    });
  } catch(e){}

  // 2. Blob Download Interceptor
  if(!window.__mcBlob){
    window.__mcBlob = true;
    var cache = {}, nameCache = {};
    var origCreate = URL.createObjectURL.bind(URL);
    URL.createObjectURL = function(blob){
      var url = origCreate(blob);
      cache[url] = blob;
      return url;
    };
    var origClick = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function(){
      if(this.download && this.href && this.href.startsWith('blob:')) nameCache[this.href] = this.download;
      return origClick.call(this);
    };
    document.addEventListener('click', function(e){
      var a = e.target && e.target.closest ? e.target.closest('a[download]') : null;
      if(a && a.download && a.href && a.href.startsWith('blob:')) nameCache[a.href] = a.download;
    }, true);
    window.__mcGetBlob = function(url, fallback, mime){
      var blob = cache[url];
      if(!blob){ MonochromeApp.onBlobError('Blob not cached'); return; }
      var name = nameCache[url] || fallback;
      var r = new FileReader();
      r.onloadend = function(){ MonochromeApp.onBlobData(r.result, name, mime); };
      r.onerror = function(){ MonochromeApp.onBlobError('FileReader failed'); };
      r.readAsDataURL(blob);
    };
  }

  // 3. Track & Playback Monitor
  if(!window.__mcTrack){
    window.__mcTrack = true;
    var lastT = '', lastA = '', lastArt = '';
    var lastState = false;

    function getTitle(){
      if (navigator.mediaSession && navigator.mediaSession.metadata && navigator.mediaSession.metadata.title) return navigator.mediaSession.metadata.title;
      var sel = ['.now-playing-title','.player-title','.track-title','.song-title','.current-track-name','.playing-title','[class*="nowPlaying"] [class*="title"]','[class*="player"] [class*="title"]'];
      for(var i=sel.length-1; i>=0; i--){
        var el = document.querySelector(sel[i]);
        if(el && el.innerText && el.innerText.trim()) return el.innerText.trim();
      }
      return document.title || 'Monochrome';
    }

    function getArtist(){
       if (navigator.mediaSession && navigator.mediaSession.metadata && navigator.mediaSession.metadata.artist) return navigator.mediaSession.metadata.artist;
       var sel = ['.now-playing-artist','.artist-name','[class*="artist"]'];
       for(var i=0; i<sel.length; i++){
          var el = document.querySelector(sel[i]);
          if(el && el.innerText && el.innerText.trim()) return el.innerText.trim();
       }
       return 'Monochrome';
    }

    function getArtwork(){
       if (navigator.mediaSession && navigator.mediaSession.metadata && navigator.mediaSession.metadata.artwork && navigator.mediaSession.metadata.artwork.length > 0) {
          return navigator.mediaSession.metadata.artwork[0].src;
       }
       var el = document.querySelector('.now-playing-art img, .album-art img, [class*="player"] img, [class*="nowPlaying"] img');
       return el ? el.src : '';
    }

    function isPlaying(){
      var a = document.querySelector('audio,video');
      return a ? !a.paused : false;
    }

    function check(){
      var t = getTitle(), a = getArtist(), art = getArtwork();
      if(t !== lastT || a !== lastA || art !== lastArt){
        lastT = t; lastA = a; lastArt = art;
        MonochromeApp.onMetadataChanged(t, a, art);
      }
      var s = isPlaying();
      if(s !== lastState){
        lastState = s;
        MonochromeApp.onPlaybackStateChanged(s);
      }
    }

    var obs = new MutationObserver(check);
    obs.observe(document.documentElement, { childList: true, subtree: true, characterData: true, attributes: true });
    setInterval(check, 3000);
    check();

    // Remote controls
    window.__mcPlay = function(){ 
      if(window.__mcHandlers && window.__mcHandlers['play']) { window.__mcHandlers['play'](); return; }
      var a=document.querySelector('audio,video'); if(a) a.play(); 
    };
    window.__mcPause = function(){ 
      if(window.__mcHandlers && window.__mcHandlers['pause']) { window.__mcHandlers['pause'](); return; }
      var a=document.querySelector('audio,video'); if(a) a.pause(); 
    };
    window.__mcNext = function(){ 
      if(window.__mcHandlers && window.__mcHandlers['nexttrack']) { window.__mcHandlers['nexttrack'](); return; }
      var btn = document.querySelector('.next-button, .player-next, [class*="next"], [id*="next"], [aria-label*="next"], [title*="next"]');
      if(btn) { btn.click(); return; }
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', shiftKey: true, bubbles: true }));
    };
    window.__mcPrev = function(){
      if(window.__mcHandlers && window.__mcHandlers['previoustrack']) { window.__mcHandlers['previoustrack'](); return; }
      var btn = document.querySelector('.prev-button, .player-prev, [class*="prev"], [id*="prev"], [aria-label*="prev"], [title*="prev"]');
      if(btn) { btn.click(); return; }
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', shiftKey: true, bubbles: true }));
    };
  }

  // 4. File System Access Polyfill
  if(!window.__mcLocal){
    window.__mcLocal = true;
    var sAI = Symbol.asyncIterator || Symbol.for('Symbol.asyncIterator');

    // Always define our own classes to ensure full control over property descriptors
    (function(){
      var FSHandle = function(){};
      Object.defineProperties(FSHandle.prototype, {
        'kind': { get: function(){ return this._kind; }, set: function(v){ this._kind = v; }, configurable: true },
        'name': { get: function(){ return this._name; }, set: function(v){ this._name = v; }, configurable: true }
      });
      var FSFileHandle = function(){};
      FSFileHandle.prototype = Object.create(FSHandle.prototype);
      var FSDirHandle = function(){};
      FSDirHandle.prototype = Object.create(FSHandle.prototype);
      if(typeof Symbol !== 'undefined' && Symbol.toStringTag){
        Object.defineProperty(FSHandle.prototype, Symbol.toStringTag, { value: 'FileSystemHandle' });
        Object.defineProperty(FSFileHandle.prototype, Symbol.toStringTag, { value: 'FileSystemFileHandle' });
        Object.defineProperty(FSDirHandle.prototype, Symbol.toStringTag, { value: 'FileSystemDirectoryHandle' });
      }
      window.FileSystemHandle = FSHandle;
      window.FileSystemFileHandle = FSFileHandle;
      window.FileSystemDirectoryHandle = FSDirHandle;
    })();

    function makeFileHandle(info){
      var h = new window.FileSystemFileHandle();
      h.kind = 'file';
      h.name = info.name;
      Object.assign(h, {
        __mcHandle: true,
        __mcUri: info.uri,
        queryPermission: async function(){ return 'granted'; },
        requestPermission: async function(){ return 'granted'; },
        isSameEntry: async function(o){ return o && (o.__mcUri === info.uri || (o.__mcHandle && o.name === info.name && o.kind === 'file')); },
        getFile: async function(){
          try {
            var url = 'https://local-file.monochrome.tf/?uri=' + encodeURIComponent(info.uri) + '&t=' + Date.now();
            var resp = await fetch(url);
            if(!resp.ok) throw new Error('Fetch failed: ' + resp.status);
            var blob = await resp.blob();
            return new File([blob], info.name, {type: blob.type || 'audio/mpeg'});
          } catch(e){
            return new Promise(function(res, rej){
              var id = 'fc_' + Date.now() + '_' + Math.random().toString(36).slice(2);
              window['__monochromeFile_' + id] = function(b64, mime){
                delete window['__monochromeFile_' + id];
                if(!b64){ rej(new Error('read failed')); return; }
                try {
                  var bin = atob(b64), bytes = new Uint8Array(bin.length);
                  for(var i=0; i<bin.length; i++) bytes[i] = bin.charCodeAt(i);
                  res(new File([new Blob([bytes], {type: mime || 'audio/mpeg'})], info.name, {type: mime || 'audio/mpeg'}));
                } catch(err){ rej(err); }
              };
              MonochromeApp.requestFileContent(info.uri, id);
            });
          }
        }
      });
      return h;
    }

    function makeDirHandle(files, hid){
      var id = hid || ('mch_' + Date.now());
      var h = new window.FileSystemDirectoryHandle();
      h.kind = 'directory';
      h.name = 'Local Files';
      Object.assign(h, {
        __mcHandle: true,
        __mcHandleId: id,
        __mcFiles: files,
        queryPermission: async function(){ return 'granted'; },
        requestPermission: async function(){ return 'granted'; },
        isSameEntry: async function(o){ return o && o.__mcHandleId === id; },
        getFileHandle: async function(n){
          var f = files.find(function(x){ return x.name === n; });
          if(!f) throw new DOMException('Not found', 'NotFoundError');
          return makeFileHandle(f);
        },
        getDirectoryHandle: async function(){ throw new DOMException('Not found', 'NotFoundError'); },
        resolve: async function(handle){
          if(handle.kind === 'file'){
            var f = files.find(function(x){ return x.name === handle.name; });
            return f ? [f.name] : null;
          }
          return null;
        }
      });
      var createIterator = function(mapFn){
        return function(){
          var i = 0;
          var it = {
            next: function(){
              if(i >= files.length) return Promise.resolve({done: true, value: undefined});
              return Promise.resolve({value: mapFn(files[i++]), done: false});
            }
          };
          it[sAI] = function(){ return this; };
          return it;
        };
      };
      h.entries = createIterator(function(f){ return [f.name, makeFileHandle(f)]; });
      h.values = createIterator(function(f){ return makeFileHandle(f); });
      h.keys = createIterator(function(f){ return f.name; });
      h[sAI] = h.entries;
      return h;
    }

    function unwrap(v){
      if(!v || typeof v !== 'object') return v;
      if(Array.isArray(v)) return v.map(unwrap);
      if(v.__mcHandle) return { __mcHandle: true, kind: v.kind, name: v.name, __mcUri: v.__mcUri, __mcHandleId: v.__mcHandleId, __mcFiles: v.__mcFiles };
      if(Object.prototype.toString.call(v) === '[object Object]'){
        var r = {};
        for(var k in v) if(Object.prototype.hasOwnProperty.call(v, k)) r[k] = unwrap(v[k]);
        return r;
      }
      return v;
    }
    var _put = IDBObjectStore.prototype.put;
    var _add = IDBObjectStore.prototype.add;
    IDBObjectStore.prototype.put = function(v, k){ return _put.call(this, unwrap(v), k); };
    IDBObjectStore.prototype.add = function(v, k){ return _add.call(this, unwrap(v), k); };

    function wrap(res){
      if(!res || typeof res !== 'object') return res;
      if(Array.isArray(res)) return res.map(wrap);
      if(res.__mcHandle){
        if(!res.getFile && !res.getFileHandle){
          if(res.kind === 'directory') return makeDirHandle(res.__mcFiles || [], res.__mcHandleId);
          return makeFileHandle({ name: res.name, uri: res.__mcUri });
        }
        return res;
      }
      if(Object.prototype.toString.call(res) === '[object Object]'){
        for(var k in res) {
          if(Object.prototype.hasOwnProperty.call(res, k)) {
            try {
              var v = wrap(res[k]);
              if(v !== res[k]) res[k] = v;
            } catch(e){}
          }
        }
      }
      return res;
    }

    var rProto = IDBRequest.prototype;
    var rDesc = Object.getOwnPropertyDescriptor(rProto, 'result');
    if(rDesc && rDesc.get){
      var _resG = rDesc.get;
      Object.defineProperty(rProto, 'result', { get: function(){ return wrap(_resG.call(this)); }, configurable: true });
    }
    if(typeof IDBCursorWithValue !== 'undefined'){
      var cProto = IDBCursorWithValue.prototype;
      var vDesc = Object.getOwnPropertyDescriptor(cProto, 'value');
      if(vDesc && vDesc.get){
        var _valG = vDesc.get;
        Object.defineProperty(cProto, 'value', { get: function(){ return wrap(_valG.call(this)); }, configurable: true });
      }
    }

    window.__mcResolveFolder = function(cbId){
      var cb = window['__mcFolderCb_' + cbId];
      if(!cb) return;
      delete window['__mcFolderCb_' + cbId];
      var files = window.__mcPendingFolder;
      window.__mcPendingFolder = null;
      if(!files || !files.length){ cb(null, 'No files'); return; }
      cb(files, null);
    };
    window.showDirectoryPicker = async function(){
      return new Promise(function(res, rej){
        var cbId = 'fp_' + Date.now();
        window['__mcFolderCb_' + cbId] = function(files, err){
          if(err || !files){ rej(new DOMException(err || 'Cancelled', 'AbortError')); return; }
          res(makeDirHandle(files));
        };
        MonochromeApp.requestFolderPicker(cbId);
      });
    };
    window.showOpenFilePicker = async function(){
      throw new DOMException('Not implemented', 'NotSupportedError');
    };
  }

  // 5. Tidal Extension Flag (Sync with v1.0.2)
  window.__tidalOriginExtension = true;

  // 6. Service Worker Auto-Update
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.addEventListener('controllerchange', function() {
      console.log('MonochromeJS: Service Worker Controller changed, reloading...');
      window.location.reload();
    });
    navigator.serviceWorker.getRegistration().then(function(reg) {
      if (reg) {
        reg.addEventListener('updatefound', function() {
          var newWorker = reg.installing;
          if (newWorker) {
            newWorker.addEventListener('statechange', function() {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                console.log('MonochromeJS: New Service Worker installed, triggering skipWaiting...');
                newWorker.postMessage({ type: 'SKIP_WAITING' });
              }
            });
          }
        });
        // Check for updates every hour
        setInterval(function() { reg.update(); }, 3600000);
      }
    });
  }

  console.log('Monochrome Hooks Injected Successfully');
})();
"""

    const val JS_PAUSE = "if(window.__mcPause) window.__mcPause(); else (function(){var a=document.querySelector('audio,video');if(a)a.pause();})()"
}
