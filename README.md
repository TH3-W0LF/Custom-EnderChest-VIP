# AlkaEnderChest

EnderChest customizado para a rede Alka* (Paper 1.21.8 / Java 21). Múltiplas
páginas por tier, senha, chave mestra e upgrades via AlkaEconomy — construído
sobre o AlkaCore (banco/GUI compartilhados). Reescrita completa do plugin
legado (Maven, Vault/EssentialsX puro, exclusivo do servidor antigo) sobre a
arquitetura Alka* atual.

## O que faz

- **Múltiplas páginas** (45 slots cada) — o número de páginas é o "tier"
  efetivo do jogador: o maior entre o tier comprado (persistido no banco) e o
  tier concedido por permissão (`permissions.tiers` no `config.yml`, ex:
  `enderchest.vip.2` → 3 páginas). VIP concede páginas automaticamente, sem
  nenhuma configuração adicional além de dar a permissão certa ao grupo.
- **Upgrade de tier**: cristal comprável via `EnderChestUpgradeGui` (`BaseGui`
  do AlkaCore) ou item físico configurável (`upgrades.tier-item`), pago em
  qualquer moeda do AlkaEconomy configurada em `economias.yml`.
- **Página nunca é apagada**: se o tier cai (VIP expirou, por exemplo), a
  página some da navegação (`EnderChestMenu` trava `maxPages`), mas os itens
  continuam salvos no banco — voltam a aparecer assim que o tier sobe de
  novo. Nada é perdido.
- **Aviso de expiração de VIP**: se o AlkaEnderChest perde páginas com o VIP
  a `<= 3 dias` de expirar, avisa o jogador no chat (uma vez por login) e
  mostra um item de aviso na própria GUI do baú — usa o tempo real restante
  via `AlkaVipsAPI` (ver Integrações abaixo), já que uma permissão crua não
  carrega data de expiração.
- **Senha opcional** (`/ec senha`): protege o baú com um prompt de chat antes
  de abrir; bloqueio progressivo por tentativas erradas (`security.punishments`
  no config). Chave mestra (`/ec chavemestra`) e comando de reset admin
  (`/aec resetpass`) para quando o jogador esquecer.
- **Auto-organização** (botão de sort na GUI) — empilha e ordena os itens da
  página atual.
- **Bloqueio de Shulker Box** dentro do baú — evita duplicação/nesting.
- **Admin**: `/aec ver <jogador>` (visualização somente-leitura, com placar de
  glass no lugar dos botões), `/aec settier`, `/aec resetpass`, `/aec reload`.

## Comandos

- `/ec` — abre o próprio baú. `/ec senha`, `/ec remover`, `/ec chavemestra`,
  `/ec entrar` — fluxo de senha.
- `/aec` — abre o próprio baú em modo admin. `/aec ver`, `/aec settier`,
  `/aec resetpass`, `/aec reload`.
- `/ececonomy` — debug da integração de economia.

## Dependências

- **AlkaCore** e **AlkaEconomy** (hard dependency, ambos) — banco de dados
  (HikariCP/SQLite/MySQL) e GUI compartilhados; moeda de upgrade configurável
  via `economias.yml` (não hardcoded).
- LuckPerms, PlaceholderAPI e AlkaVips são soft-dependencies opcionais.

## Integração com AlkaVips

`hook/AlkaVipsHook` (softdepend, 100% reflexão — nunca importa
`com.alkacode.vips.*` direto, mesmo padrão do resto do ecossistema Alka*)
consulta `AlkaVipsAPI#getActiveVip` só para saber quanto tempo falta pro VIP
ativo do jogador expirar, usado exclusivamente no aviso dos 3 dias finais. O
acesso às páginas em si continua 100% via permissão (`permissions.tiers`),
nunca decidido pelo hook — sem AlkaVips instalado, o aviso simplesmente não
aparece, e o resto do plugin funciona normalmente. Resolução do hook adiada 1
tick no `onEnable` (softdepend não garante ordem estrita de carregamento).

## Débitos conhecidos

- Reescrita a partir do plugin legado (Maven/Vault puro) — funcionalidades
  muito específicas do servidor antigo (DrakkarMC) que não foram citadas
  explicitamente na migração podem ter ficado pra trás.
