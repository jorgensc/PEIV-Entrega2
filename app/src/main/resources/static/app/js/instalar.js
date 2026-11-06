// instalacao do PWA na tela inicial
// Android/Chrome/Edge: usa o evento beforeinstallprompt
// iPhone (Safari): nao tem esse evento, entao mostra o passo a passo
import { esc, ic, toast } from './util.js';

let promptGuardado = null;
const ouvintes = new Set();

const ehIos = /iphone|ipad|ipod/i.test(navigator.userAgent);
const instalado = () => matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;

window.addEventListener('beforeinstallprompt', e => {
    e.preventDefault();
    promptGuardado = e;
    ouvintes.forEach(f => f());
});

window.addEventListener('appinstalled', () => {
    promptGuardado = null;
    ouvintes.forEach(f => f());
    toast('JCondo instalado na tela inicial!');
});

function disponivel() {
    return !instalado() && (promptGuardado !== null || ehIos);
}

async function instalar() {
    if (promptGuardado) {
        promptGuardado.prompt();
        await promptGuardado.userChoice;
        promptGuardado = null;
        ouvintes.forEach(f => f());
    } else if (ehIos) {
        alert('Para instalar no iPhone: toque no botão Compartilhar do Safari e depois em "Adicionar à Tela de Início".');
    }
}

function acompanhar(el, desenhar) {
    const atualizar = () => {
        if (!el.isConnected) {
            ouvintes.delete(atualizar);
            return;
        }
        desenhar();
    };
    ouvintes.add(atualizar);
    desenhar();
}

export const instalacao = {
    // banner na tela inicial
    desenharBanner(el) {
        if (!el) return;
        acompanhar(el, () => {
            const dispensado = localStorage.getItem('jcondo.banner-instalar') === 'nao';
            if (!disponivel() || dispensado) {
                el.innerHTML = '';
                return;
            }
            el.innerHTML = `
                <div class="banner">
                    ${ic('instalar')}
                    <div class="meio">
                        <strong>Instale o JCondo</strong>
                        <small>${esc(ehIos ? 'Adicione à tela de início do iPhone.' : 'Abre mais rápido e funciona sem internet.')}</small>
                    </div>
                    <button class="botao" type="button" data-instalar>Instalar</button>
                    <button class="btn-icone" type="button" data-dispensar aria-label="Dispensar">${ic('x')}</button>
                </div>`;
            el.querySelector('[data-instalar]').onclick = instalar;
            el.querySelector('[data-dispensar]').onclick = () => {
                localStorage.setItem('jcondo.banner-instalar', 'nao');
                el.innerHTML = '';
            };
        });
    },

    // linha no perfil
    desenharLinha(el) {
        if (!el) return;
        acompanhar(el, () => {
            el.innerHTML = disponivel()
                ? `<button class="linha-acao" type="button" data-instalar>${ic('instalar')}<span>Instalar app na tela inicial</span>${ic('seta')}</button>`
                : '';
            el.querySelector('[data-instalar]')?.addEventListener('click', instalar);
        });
    }
};
