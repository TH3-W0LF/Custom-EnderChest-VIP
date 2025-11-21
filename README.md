# 🐉 CustomEnderChest VIP

Plugin de EnderChest personalizado para Minecraft com sistema de múltiplas páginas, upgrades e integração com yEconomias.

## ✨ Funcionalidades

- ✅ **Múltiplas Páginas**: Sistema de tiers (2-11 páginas)
- ✅ **Sistema de Upgrades**: Compre cristais de tier para expandir seu EnderChest
- ✅ **Itens Físicos**: Cristais de tier podem ser comercializados entre jogadores
- ✅ **Sistema de Senha**: Proteja seu EnderChest com senha
- ✅ **Auto-Organização**: Organize seus itens automaticamente
- ✅ **Integração Vault**: Sistema de economia via Vault
- ✅ **Sistema de Permissões**: Tiers baseados em permissões VIP

## 📋 Requisitos

- **Minecraft**: 1.21+
- **Vault**: Plugin recomendado (softdepend)
- **Plugin de Economia**: Qualquer plugin compatível com Vault (EssentialsX, CMI, etc.)

## 🚀 Instalação

1. Baixe o arquivo `CustomEnderChest-1.0-SNAPSHOT.jar` da pasta `target/`
2. Copie para a pasta `plugins/` do seu servidor
3. Certifique-se de que o **Vault** e um **plugin de economia** estão instalados
4. Reinicie o servidor

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

O plugin usa **Vault** para integração com sistemas de economia:

- Conecta automaticamente ao provider de economia via Vault
- Funciona com qualquer plugin de economia compatível (EssentialsX, CMI, etc.)
- Todos os preços são configuráveis no `economias.yml`
- Se o Vault não estiver instalado, o plugin funciona sem economia

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
├── economy/           # Sistema de economia (Vault)
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
- Verifique se o Vault está instalado
- Confirme que um plugin de economia compatível está instalado e funcionando

### Cristais não funcionam

- Verifique se o item é PAPER com CustomModelData 1000
- Veja os logs ao clicar no item
- Confirme que o nome contém "Tier"

## 📄 Licença e Copyright

**Copyright (c) 2024 MestreBR - Todos os direitos reservados.**

Este software e código-fonte são propriedade exclusiva de **MestreBR (Desenvolvedor)**.

Este plugin foi desenvolvido **exclusivamente para uso no servidor DrakkarMC** (www.drakkarmc.com.br).

### ⚠️ Restrições

É **PROIBIDO**:
- Distribuir, copiar, modificar ou vender este plugin sem autorização expressa do desenvolvedor
- Usar este plugin em outros servidores sem permissão
- Remover ou alterar notificações de copyright
- Fazer engenharia reversa ou descompilar o código

Para solicitar permissão de uso ou modificação, entre em contato com o desenvolvedor.

## 👥 Desenvolvedor

- **MestreBR** - Desenvolvedor e Proprietário

## 🔗 Links

- **Servidor**: www.drakkarmc.com.br
- **GitHub**: https://github.com/TH3-W0LF/Custom-EnderChest-VIP

---

**Versão**: 1.0  
**Última atualização**: 2024  
**Servidor Exclusivo**: DrakkarMC (www.drakkarmc.com.br)

