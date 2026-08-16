<div align="center">

# AlkaEnderChest

### Ender Chest customizado, com tiers e progressão

Múltiplas páginas por tier, segurança por senha e upgrades pagos em qualquer
moeda do AlkaEconomy — construído sobre o **AlkaCore**.

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.13-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

</div>

---

## 📋 Sobre o Projeto

O **AlkaEnderChest** transforma o baú do fim padrão numa progressão de
verdade: cada jogador desbloqueia mais páginas conforme evolui, com
segurança opcional por senha e integração completa com a economia e o VIP
da rede.

## ✨ Funcionalidades Principais

- 📦 **Múltiplas páginas** — o número de páginas escala com o tier do
  jogador, seja comprado ou concedido automaticamente por VIP.
- 💎 **Upgrade de tier** — compra direto na GUI ou via item físico, pago em
  qualquer moeda configurada no AlkaEconomy.
- 🔒 **Nada se perde** — se o tier cai, a página só some da navegação; os
  itens continuam salvos e voltam assim que o tier sobe de novo.
- ⏰ **Aviso de expiração de VIP** — avisa o jogador no chat e na própria GUI
  quando o VIP está perto de expirar e páginas podem ser perdidas.
- 🔑 **Senha opcional** — protege o baú com um prompt de chat, com chave
  mestra e reset administrativo pra quando o jogador esquecer.
- ✨ **Auto-organização** — botão de sort que empilha e ordena os itens da
  página atual.
- 🚫 **Bloqueio de Shulker Box** dentro do baú.
- 🛠️ **Ferramentas administrativas** — visualização somente-leitura do baú
  de qualquer jogador, definição manual de tier e reset de senha.

## 🎮 Comandos

| Comando | Descrição | Permissão |
| --- | --- | --- |
| `/ec` | Abre seu Ender Chest | — |
| `/ec senha` | Protege o baú com senha | — |
| `/ec remover` | Remove a senha atual | — |
| `/ec chavemestra` | Abre o baú ignorando a senha | — |
| `/ec entrar` | Informa a senha pra destravar o baú | — |
| `/aec` | Abre o baú em modo administrativo | `admin` |
| `/aec ver <jogador>` | Visualiza o baú de outro jogador (somente leitura) | `admin` |
| `/aec settier <jogador> <tier>` | Define o tier manualmente | `admin` |
| `/aec resetpass <jogador>` | Reseta a senha de um jogador | `admin` |
| `/aec reload` | Recarrega as configurações | `admin` |

## 🔗 Integrações

Construído sobre o **AlkaCore** e o **AlkaEconomy** (moeda de upgrade
configurável). Integração opcional com **AlkaVips** (aviso de expiração) e
suporte a **LuckPerms** e **PlaceholderAPI**.

## 🔧 Tecnologias Utilizadas

- **Java 21** · **Paper API 1.21.8**
- **AlkaCore** (banco de dados e GUI compartilhados)
- **AlkaEconomy** (moedas de upgrade)

## ⚙️ Instalação

1. Instale o **AlkaCore** e o **AlkaEconomy** antes (dependências obrigatórias).
2. Coloque `AlkaEnderChest.jar` na pasta `plugins/` do servidor.
3. Reinicie o servidor.
4. Configure tiers, preços e segurança em `plugins/AlkaEnderChest/config.yml`.

## 🔐 Permissões

| Permissão | Descrição |
| --- | --- |
| `enderchest.vip.<n>` | Concede páginas extras automaticamente por tier |
| `alkaenderchest.admin` | Acesso aos comandos administrativos (`/aec`) |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte do ecossistema**: `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
