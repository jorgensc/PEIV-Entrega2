// service worker do JCondo
// - arquivos do app: cache primeiro (abre instantaneo e funciona offline)
// - API (GET): rede primeiro, cai pro cache quando nao tem internet
// - POST/PUT/PATCH/DELETE: sempre rede (a fila de ocorrencias fica no app)

const VERSAO = 'v2.1.0';
const CACHE_APP = `jcondo-app-${VERSAO}`;
const CACHE_API = 'jcondo-api';

const ARQUIVOS = [
    './',
    './index.html',
    './manifest.webmanifest',
    './css/app.css',
    './js/app.js',
    './js/api.js',
    './js/util.js',
    './js/telas.js',
    './js/instalar.js',
    './icons/icon-192.png',
    './icons/icon-512.png',
    './icons/maskable-512.png',
    './icons/apple-touch-icon.png',
    './icons/favicon.svg'
];

self.addEventListener('install', evento => {
    evento.waitUntil(
        caches.open(CACHE_APP)
            .then(cache => cache.addAll(ARQUIVOS.map(url => new Request(url, { cache: 'reload' }))))
    );
});

self.addEventListener('activate', evento => {
    evento.waitUntil((async () => {
        const nomes = await caches.keys();
        await Promise.all(nomes
            .filter(n => n.startsWith('jcondo-app-') && n !== CACHE_APP)
            .map(n => caches.delete(n)));
        await self.clients.claim();
    })());
});

self.addEventListener('message', evento => {
    if (evento.data?.tipo === 'pular-espera') {
        self.skipWaiting();
    }
    if (evento.data?.tipo === 'limpar-api') {
        caches.delete(CACHE_API);
    }
});

self.addEventListener('fetch', evento => {
    const req = evento.request;
    if (req.method !== 'GET') return;

    const url = new URL(req.url);
    if (url.origin !== self.location.origin) return;

    if (url.pathname.startsWith('/api/')) {
        evento.respondWith(redePrimeiro(req));
        return;
    }

    if (url.pathname.startsWith('/app/')) {
        if (req.mode === 'navigate') {
            // abrir o app: tenta a rede (pega versao nova) e usa o index salvo se estiver offline
            evento.respondWith(
                fetch(req).catch(() => caches.match('./index.html'))
            );
            return;
        }
        evento.respondWith(cachePrimeiro(req));
    }
});

async function cachePrimeiro(req) {
    const salvo = await caches.match(req, { ignoreSearch: true });
    if (salvo) return salvo;
    const resposta = await fetch(req);
    if (resposta.ok) {
        const cache = await caches.open(CACHE_APP);
        cache.put(req, resposta.clone());
    }
    return resposta;
}

async function redePrimeiro(req) {
    const cache = await caches.open(CACHE_API);
    try {
        const resposta = await fetch(req);
        if (resposta.ok) {
            cache.put(req, resposta.clone());
        }
        return resposta;
    } catch {
        const salvo = await cache.match(req, { ignoreVary: true });
        if (salvo) {
            // marca pro app mostrar o selo de "dados salvos"
            const headers = new Headers(salvo.headers);
            headers.set('X-JCondo-Offline', '1');
            return new Response(await salvo.blob(), { status: salvo.status, headers });
        }
        return new Response(JSON.stringify({
            status: 503,
            title: 'Offline',
            detail: 'Sem conexão e sem dados salvos desta tela.',
            offline: true
        }), {
            status: 503,
            headers: { 'Content-Type': 'application/problem+json', 'X-JCondo-Offline': '1' }
        });
    }
}
