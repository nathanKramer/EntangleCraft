package entanglecraft;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import universalelectricity.core.UniversalElectricity;
import universalelectricity.core.electricity.ElectricityConnections;

import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.world.World;
import entanglecraft.*;
import entanglecraft.SoundHandling.LambdaSoundHandler;
import entanglecraft.blocks.*;
import entanglecraft.blocks.tileEntity.TileEntityGenericDestination;
import entanglecraft.gui.EnumGui;
import entanglecraft.generation.*;
import entanglecraft.items.EntangleCraftItems;
import entanglecraft.items.ItemChanneled;
import entanglecraft.items.ItemDevice;
import entanglecraft.items.ItemLambda;
import entanglecraft.items.ItemShard;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.asm.transformers.SideTransformer;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.NetworkMod.SidedPacketHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = "EntangleCraft", name = "EntangleCraft", version = "1.0.3b")
@NetworkMod(
clientSideRequired = true, 
serverSideRequired = false, 
clientPacketHandlerSpec = @SidedPacketHandler(channels = { "EntangleCraft" }, 
packetHandler = ClientPacketHandler.class), 
serverPacketHandlerSpec = @SidedPacketHandler(channels = { "EntangleCraft" }, 
packetHandler = ServerPacketHandler.class), 
connectionHandler = EntangleCraft.class)


public class EntangleCraft implements IConnectionHandler {
	@Instance
	public static EntangleCraft instance;
	public static final String NAME = "EntangleCraft";
	public static boolean universalElectricity = true;
	
	public static final int MAJOR_VERSION = 1;
	public static final int MINOR_VERSION = 0;
	public static final int REVISION = 2;
	public static final String VERSION = "" + MAJOR_VERSION + "." + MINOR_VERSION + "." + REVISION + "b";

	@SidedProxy(clientSide = "entanglecraft.ClientProxy", serverSide = "entanglecraft.CommonProxy")
	public static CommonProxy proxy;

	private static ArrayList destinations = new ArrayList();
	public static ArrayList[] channelDests = { new ArrayList(),
			new ArrayList(), new ArrayList(), new ArrayList() };

	public static DistanceHandler dhInstance = new DistanceHandler();

	@PreInit
	public void preLoad(FMLPreInitializationEvent event) {
		proxy.registerPreLoad();
		MinecraftForge.EVENT_BUS.register(dhInstance);
		
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		configure(config);
	}

	@Init
	public void load(FMLInitializationEvent event) {
		instance = this;
		proxy.registerOnLoad();
		NetworkRegistry.instance().registerGuiHandler(this, proxy);
		GameRegistry.registerWorldGenerator(new WorldGenFunctions());
		
		if (universalElectricity)
		{
			UniversalElectricity.register(this, UniversalElectricity.MAJOR_VERSION, UniversalElectricity.MINOR_VERSION, UniversalElectricity.REVISION_VERSION, false);
		}
	}

	public void configure(Configuration config) {

        // loading the configuration from its file
        config.load();
        
        WorldGenFunctions.worldGenerationEnabled = config.get(Configuration.CATEGORY_GENERAL, "isWorldGenerationEnabled", true).getBoolean(true);
        
        EntangleCraftBlocks.configureIDs(config);
        EntangleCraftItems.configureIDs(config);
        
        // saving the configuration to its file
        config.save();
	}
	
	public static Destination closestDestToPlayer(EntityPlayer playerEntity, ArrayList dests) {
		Destination destination = null;
		double[] playerPoints = new double[] { playerEntity.posX, playerEntity.posY,
				playerEntity.posZ };
		
		destination = closestDestToCoord(playerPoints, dests);
		return destination;
	}
	
	public static Destination closestDestToCoord(double[] coords, ArrayList dests) {
		Destination destination = null;
		
		double minDistance;
		Iterator iterator;
		if (dests.size() != 0) 
		{
			destination = (Destination) dests.get(0);
			minDistance = getDistance(coords,
					destination.destinationCoords);

			for (iterator = dests.iterator(); iterator.hasNext();) 
			{
				Object points = iterator.next();
				Destination newPoints = (Destination) points;
				double newDistance = getDistance(coords,
						newPoints.destinationCoords);
				if (newDistance < minDistance) 
				{
					minDistance = newDistance;
					destination = newPoints;
				}
			}
			System.out.println("Minimum distance calculated as "
					+ minDistance);
		}
		return destination;
	}
	
	public static ArrayList getDestsFromChannelAndDimension(ArrayList<Destination>[] destinations, int channel, int dimension){
		ArrayList<Destination> dests = new ArrayList<Destination>();
		ArrayList<Destination> channelDests = destinations[channel];
		System.out.println("Getting destinations of channel " + channel + " and in the " + (dimension == 0 ? "default dimension " : dimension == -1 ? "nether dimension" : dimension + " dimension"));
		
		for (Destination d : channelDests)
		{
			if (d.dimension == dimension)
			{
				dests.add(d);
			}
		}
		System.out.println("Returning a list of size " + dests.size());
		return dests;
	}
	
	public static void teleport(EntityPlayer parPlayer, int channel) {
		// This method is disgusting to read just hide it
		if (!parPlayer.worldObj.isRemote) 
		{
			ArrayList dests = getDestsFromChannelAndDimension(channelDests, channel, parPlayer.dimension);
			if (dests.size() != 0) {
				Destination dest = closestDestToPlayer(parPlayer, dests);
				double[] destinationPoints = dest.destinationCoords;

				double[] playerPoints = { parPlayer.posX,
						parPlayer.posY, parPlayer.posZ };
				double amount = getDistance(destinationPoints, playerPoints);
				dhInstance.addToDistance(channel, amount);

				if (parPlayer instanceof EntityPlayerMP) {
					EntityPlayerMP thePlayer = (EntityPlayerMP) parPlayer;
					// Server-side teleports seem to teleport the player too
					// high so I'm subtracting 1.65 from the usual height
					try {
						int[] closBlock = dest.blockCoords;
						World theWorld = parPlayer.worldObj;
						TileEntityGenericDestination destEntity = (TileEntityGenericDestination) theWorld
								.getBlockTileEntity(closBlock[0], closBlock[1],
										closBlock[2]);
						if (destEntity.teleportsEarned != 0)
							destEntity.changeTeleportsEarned(-1);
						
						thePlayer.playerNetServerHandler.setPlayerLocation(
								destinationPoints[0],
								destinationPoints[1] - 1.65,
								destinationPoints[2],
								parPlayer.rotationYaw,
								parPlayer.rotationPitch);
						
						
						ServerPacketHandler.playTPSoundToClients(thePlayer,
								destinationPoints, "teleport");
						ServerPacketHandler.spawnParticleToClients(
								destinationPoints, "largeexplosion");
						ServerPacketHandler.spawnParticleToClients(
								playerPoints, "largeexplosion");

					
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void addDestination(Destination dest) {
		int channel = dest.channel;
		
		if (!channelDests[channel].contains(dest))
		{
			channelDests[channel].add(dest);
		}
		
		if (!destinations.contains(dest))
		{
			destinations.add(dest);
		}

	}

	public static void emptyDestinationList() {
		destinations = new ArrayList();
	}

	public static void removeDestination(Destination destToRemove) {
		int channel = destToRemove.channel;

		channelDests[channel].remove(destToRemove);
		destinations.remove(destToRemove);
	}

	public static double getDistance(double[] a, double[] b) {
		double x = a[0];
		double y = a[1];
		double z = a[2];
		double x0 = b[0];
		double y0 = b[1];
		double z0 = b[2];
		return Math.sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0) + (z - z0)
				* (z - z0));
	}

	@Override
	public void playerLoggedIn(Player player, NetHandler netHandler,
			INetworkManager manager) {
		// TODO Auto-generated method stub

	}

	@Override
	public String connectionReceived(NetLoginHandler netHandler,
			INetworkManager manager) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void connectionOpened(NetHandler netClientHandler, String server,
			int port, INetworkManager manager) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionOpened(NetHandler netClientHandler,
			MinecraftServer server, INetworkManager manager) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionClosed(INetworkManager manager) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clientLoggedIn(NetHandler clientHandler,
			INetworkManager manager, Packet1Login login) {
		// TODO Auto-generated method stub

	}

}
