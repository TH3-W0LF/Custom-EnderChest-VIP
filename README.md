# 🐉 CustomEnderChest VIP

Plugin de EnderChest personalizado para Minecraft com sistema de múltiplas páginas, upgrades e integração com yEconomias.

## ✨ Funcionalidades

- ✅ **Múltiplas Páginas**: Sistema de tiers (2-11 páginas)
- ✅ **Sistema de Upgrades**: Compre cristais de tier para expandir seu EnderChest
- ✅ **Itens Físicos**: Cristais de tier podem ser comercializados entre jogadores
- ✅ **Sistema de Senha**: Proteja seu EnderChest com senha
- ✅ **Auto-Organização**: Organize seus itens automaticamente
- ✅ **Integração yEconomias**: Hook direto com economia "drakonio" (sem Vault)
- ✅ **Sistema de Permissões**: Tiers baseados em permissões VIP

## 📋 Requisitos

- **Minecraft**: 1.21+
- **yPlugins/yEconomias**: Plugin obrigatório
- **Economia "drakonio"**: Deve estar configurada no yEconomias

## 🚀 Instalação

1. Baixe o arquivo `CustomEnderChest-1.0-SNAPSHOT.jar` da pasta `target/`
2. Copie para a pasta `plugins/` do seu servidor
3. Certifique-se de que o **yPlugins/yEconomias** está instalado
4. Verifique se existe `/plugins/yEconomias/economias/drakonio.yml`
5. Reinicie o servidor

## ⚙️ Configuração

### economias.yml

O arquivo será criado automaticamente em `plugins/CustomEnderChest/economias.yml`:

```yaml
economy-name: "drakonio"
display-name: "Drakonio"
abbreviated: "drakonio"

upgrade-prices:
  2: 1000
  3: 2000
  4: 4000
  5: 8000
  6: 16000
  7: 32000
  8: 64000
  9: 128000
  10: 256000
  11: 512000
```

### config.yml

Configure os botões da GUI, mensagens e sistema de segurança em `plugins/CustomEnderChest/config.yml`.

## 🎮 Comandos

- `/ec` ou `/bau` - Abre seu EnderChest
- `/aec <jogador>` - Abre o EnderChest de outro jogador (admin)
- `/ec senha <senha> <repetir>` - Define/remove senha do EnderChest
- `/ececonomy` - Debug do sistema de economia (admin)

## 🔐 Permissões

- `meuplugin.vip.escudeiro` - Tier 1 (36 slots)
- `meuplugin.vip.cavaleiro` - Tier 2 (2 páginas)
- `meuplugin.vip.lorde` - Tier 3 (3 páginas)
- `meuplugin.vip.drakkar` - Tier 5 (5 páginas)
- `meuplugin.admin` - Comandos administrativos

## 💰 Sistema de Economia

O plugin usa **hook direto** do yEconomias, sem passar pelo Vault:

- Conecta automaticamente à economia "drakonio"
- Se não conseguir conectar, o plugin é desabilitado
- Todos os preços são configuráveis no `economias.yml`

## 📦 Como Funciona

### Comprar Upgrades

1. Abra o EnderChest (`/ec`)
2. Clique no botão **"Upgrades"** (esmeralda)
3. Escolha o tier desejado
4. Os drakonios serão descontados automaticamente
5. O **Cristal de Tier** aparecerá no seu inventário

### Ativar Upgrades

1. Segure o **Cristal de Tier** na mão
2. **Clique com botão direito** (no ar ou em qualquer bloco)
3. O cristal será consumido
4. A nova página será liberada automaticamente!

## 🛠️ Desenvolvimento

### Compilar

```bash
mvn clean package
```

O arquivo JAR será gerado em `target/CustomEnderChest-1.0-SNAPSHOT.jar`

### Estrutura do Projeto

```
src/main/java/org/dark/customenderchest/
├── commands/          # Comandos do plugin
├── database/          # Gerenciamento do banco de dados
├── economy/           # Sistema de economia (yEconomias)
├── listeners/         # Event listeners
├── manager/           # Gerenciadores principais
└── utils/             # Utilitários
```

## 📝 Logs

O plugin gera logs extensivos para debug:

- ✅ Conexão com economia
- ✅ Compras de upgrades
- ✅ Ativação de cristais
- ✅ Operações de banco de dados

## 🐛 Troubleshooting

### Plugin não carrega

- Verifique se o yPlugins/yEconomias está instalado
- Veja os logs do servidor para erros

### Economia não conecta

- Execute `/ececonomy` para ver o status
- Verifique se `drakonio.yml` existe no yEconomias
- Confirme que o `plugin-id` está correto

### Cristais não funcionam

- Verifique se o item é PAPER com CustomModelData 1000
- Veja os logs ao clicar no item
- Confirme que o nome contém "Tier"

## 📄 Licença

Este projeto é privado e de propriedade de TH3-W0LF.

## 👥 Autores

- **Dark** - Desenvolvimento
- **MestreBR** - Desenvolvimento e testes

## 🔗 Links

- **GitHub**: https://github.com/TH3-W0LF/Custom-EnderChest-VIP

---

**Versão**: 1.0  
**Última atualização**: 2024

