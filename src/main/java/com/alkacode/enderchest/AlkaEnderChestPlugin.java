package com.alkacode.enderchest;

import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.plugin.AlkaPlugin;
import com.alkacode.economy.AlkaEconomyPlugin;
import com.alkacode.enderchest.command.CommandAEC;
import com.alkacode.enderchest.command.CommandEC;
import com.alkacode.enderchest.command.CommandEconomy;
import com.alkacode.enderchest.database.EnderChestRepository;
import com.alkacode.enderchest.economy.EconomyService;
import com.alkacode.enderchest.hook.AlkaVipsHook;
import com.alkacode.enderchest.hook.ItemsAdderFontHook;
import com.alkacode.enderchest.listener.EnderChestListener;
import com.alkacode.enderchest.listener.PasswordChatGuardListener;
import com.alkacode.enderchest.manager.EnderChestManager;
import com.alkacode.enderchest.service.EnderChestService;
import com.alkacode.enderchest.util.LogUtils;
import com.alkacode.enderchest.util.Messages;
import com.alkacode.enderchest.util.PasswordGate;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Base do EnderChest customizado sobre o AlkaCore (banco/HikariCP via
 * api.getDatabase(), sem DatabaseManager proprio - ver {@link EnderChestRepository})
 * e o AlkaEconomy (moeda configuravel via economias.yml, padrao alkarion). Estende
 * {@link AlkaPlugin} em vez de JavaPlugin
 * direto: garante que {@link AlkaAPI#get()} ja esta pronta quando
 * {@link #onPluginEnable()} roda (via `depend: [AlkaCore, AlkaEconomy]` no
 * plugin.yml) e evita registrar o GuiListener de novo (o Core ja faz isso uma unica
 * vez, usado aqui so pela loja de upgrades - o menu de conteudo do EC continua com
 * InventoryHolder proprio).
 */
public final class AlkaEnderChestPlugin extends AlkaPlugin {

    @Override
    protected void onPluginEnable() {
        LogUtils.init(this);
        Messages messages = new Messages(this);

        AlkaAPI api = getAlkaAPI();

        // AlkaEconomy e depend obrigatorio (plugin.yml) - Bukkit garante que ja esta
        // habilitado antes deste onPluginEnable rodar.
        if (!(getServer().getPluginManager().getPlugin("AlkaEconomy") instanceof AlkaEconomyPlugin alkaEconomy)) {
            getLogger().severe("AlkaEconomy e obrigatorio e nao foi encontrado. Desativando.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        EnderChestRepository repository = new EnderChestRepository(api.getDatabase(), getLogger());
        EnderChestManager enderChestManager = new EnderChestManager(this, repository);

        EconomyService economyService = new EconomyService(this, alkaEconomy.getEconomyManager());

        PasswordGate passwordGate = new PasswordGate(this, repository, enderChestManager, messages);

        // AlkaVips (softdepend) nao tem ordem garantida de onEnable - mesma licao ja
        // aplicada no AlkaDrop/AlkaMines (bug real 2026-08-09): resolve 1 tick depois,
        // nunca sincrono aqui.
        AtomicReference<AlkaVipsHook> alkaVipsHookRef = new AtomicReference<>(null);
        Bukkit.getScheduler().runTask(this, () -> alkaVipsHookRef.set(AlkaVipsHook.tryHook(getLogger())));

        // ItemsAdder (softdepend) - mesma licao de ordem de carregamento do AlkaVips acima:
        // resolve 1 tick depois, nunca sincrono aqui.
        AtomicReference<ItemsAdderFontHook> itemsAdderFontHookRef = new AtomicReference<>(null);
        Bukkit.getScheduler().runTask(this, () -> itemsAdderFontHookRef.set(ItemsAdderFontHook.tryHook(getLogger())));

        EnderChestService enderChestService = new EnderChestService(this, repository, passwordGate, enderChestManager,
                messages, alkaVipsHookRef::get, itemsAdderFontHookRef::get);
        passwordGate.setEnderChestService(enderChestService);

        EnderChestListener listener = new EnderChestListener(this, enderChestService, enderChestManager,
                repository, economyService, messages, alkaVipsHookRef::get);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(new PasswordChatGuardListener(passwordGate, messages), this);

        getCommand("ec").setExecutor(new CommandEC(this, enderChestService, repository, passwordGate, messages));
        getCommand("aec").setExecutor(new CommandAEC(this, repository, enderChestService, economyService, messages));
        getCommand("ececonomy").setExecutor(new CommandEconomy(economyService, messages));

        getLogger().info("AlkaEnderChest habilitado (moeda: " + economyService.getCurrencyName() + ").");
    }

    @Override
    protected void onPluginDisable() {
        // O DatabaseProvider (conexao/pool) e do AlkaCore - fechado pelo proprio Core no seu onDisable, nunca aqui.
    }
}
