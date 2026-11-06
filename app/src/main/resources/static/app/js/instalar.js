// instalacao do PWA na tela inicial (opcao no perfil)
// Android/Chrome/Edge: evento beforeinstallprompt; iPhone: passo a passo do Safari
import { ic } from './util.js';

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
});

async function instalar() {
    if (promptGuardado) {
        promptGuardado.prompt();
        await promptGuardado.userChoice;
        promptGuardado = null;
        ouvintes.forEach(f => f());
    } else if (ehIos) {
        alert('No Safari, toque em Compartilhar e depois em "Adicionar à Tela de Início".');
    }
}

export const instalacao = {
    desenharLinha(el) {
        const desenhar = () => {
            if (!el.isConnected) {
                ouvintes.delete(desenhar);
                return;
            }
            const disponivel = !instalado() && (promptGuardado !== null || ehIos);
            el.innerHTML = disponivel
                ? `<button class="linha-acao" type="button">${ic('instalar')}<span>Instalar app</span></button>`
                : '';
            el.querySelector('button')?.addEventListener('click', instalar);
        };
        ouvintes.add(desenhar);
        desenhar();
    }
};
