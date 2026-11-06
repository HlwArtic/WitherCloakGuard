package com.example.wcguard;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


@Mod(modid = WitherCloakGuard.MODID,
     name = "Wither Cloak Guard",
     version = WitherCloakGuard.VERSION,
     clientSideOnly = true,
     acceptedMinecraftVersions = "[1.8.9]")
public class WitherCloakGuard {

    public static final String MODID = "wcguard";
    public static final String VERSION = "1.0";

    private static final int MIN_DELAY = 50;
    private static final int MAX_DELAY = 2000;

    private static Configuration config;
    private static Property delayProp;
    private static Property enabledProp;

    private static volatile int delayMs = 300;
    private static volatile boolean enabled = true;

    
    private static long lastAccepted = 0L;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        enabledProp = config.get(Configuration.CATEGORY_GENERAL, "enabled", true,
                "Turn the guard on or off.");
        delayProp = config.get(Configuration.CATEGORY_GENERAL, "delayMs", 300,
                "Right clicks closer together than this many milliseconds are blocked while holding the Wither Cloak Sword.",
                MIN_DELAY, MAX_DELAY);
        enabled = enabledProp.getBoolean();
        delayMs = clamp(delayProp.getInt());
        if (config.hasChanged()) config.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new GuardCommand());
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!enabled || !event.buttonstate) return; 
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) return;

        
        if (event.button != mc.gameSettings.keyBindUseItem.getKeyCode() + 100) return;

        if (!isWitherCloak(mc.thePlayer.getHeldItem())) return;

        long now = System.currentTimeMillis();
        if (now - lastAccepted < delayMs) {
            event.setCanceled(true); 
        } else {
            lastAccepted = now;
        }
    }

    private static boolean isWitherCloak(ItemStack stack) {
        if (stack == null) return false;

        
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("ExtraAttributes", 10)) {
            String id = tag.getCompoundTag("ExtraAttributes").getString("id");
            if ("WITHER_CLOAK_SWORD".equals(id)) return true;
        }

      
        String name = EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName());
        return name != null && name.contains("Wither Cloak Sword");
    }

    private static int clamp(int v) {
        return Math.max(MIN_DELAY, Math.min(MAX_DELAY, v));
    }

    private static void say(ICommandSender sender, String msg) {
        sender.addChatMessage(new ChatComponentText(
                EnumChatFormatting.AQUA + "[WCGuard] " + EnumChatFormatting.RESET + msg));
    }

   
    public static class GuardCommand extends CommandBase {
        @Override
        public String getCommandName() {
            return "wcguard";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/wcguard [on|off|<milliseconds " + MIN_DELAY + "-" + MAX_DELAY + ">]";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length == 0) {
                say(sender, (enabled ? "ON" : "OFF") + ", delay " + delayMs + " ms. Usage: " + getCommandUsage(sender));
                return;
            }

            String a = args[0];
            if (a.equalsIgnoreCase("on") || a.equalsIgnoreCase("off")) {
                enabled = a.equalsIgnoreCase("on");
                enabledProp.set(enabled);
                config.save();
                say(sender, "Guard " + (enabled ? "enabled." : "disabled."));
                return;
            }

            try {
                int ms = clamp(Integer.parseInt(a));
                delayMs = ms;
                delayProp.set(ms);
                config.save();
                say(sender, "Delay set to " + ms + " ms.");
            } catch (NumberFormatException e) {
                say(sender, "Usage: " + getCommandUsage(sender));
            }
        }
    }
}
