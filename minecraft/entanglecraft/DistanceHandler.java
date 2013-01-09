package entanglecraft;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;

public class DistanceHandler {
	private static Integer[] distances = { 0, 0, 0, 0 };
	
	public static int[] dungeonCoords;
	public static int[] skyFortressA;
	public static int[] skyFortressB;
	public static int[] skyFortressC;
	
	public int oreGenCount = 0;
	
	public static Integer getDistance(int index) {
		return distances[index];
	}

	public static void addToDistance(int index, double amount) {
		distances[index] += (int) amount;
		//onDistanceChanged(index);
	}

	public static void setDistance(int index, double amount) {
		distances[index] = (int) amount;
		//onDistanceChanged(index);
	}

	public static String getStringDistance(int index) {
		Integer distance = distances[index];
		Float distanceKbz = (float) distance;
		String postFix = "";
		if (distance > 100) {
			distanceKbz = (distance / 1024F);
			postFix = " Kbz";
		}
		if (distance > 1048576) {
			distanceKbz = (distance / 1048576F);
			postFix = " Mbz";
		}
		DecimalFormat dec = new DecimalFormat("###.#");
		return dec.format(distanceKbz) + postFix;
	}

	public static void subtractDistance(int index, double amount) {
		if (distances[index] >= (int) amount) {
			distances[index] -= (int) amount;
			//onDistanceChanged(index);
		}
	}
	
	public static double calculate3dDistance(double[] a, double[] b) {
		double x = a[0];
		double y = a[1];
		double z = a[2];
		double x0 = b[0];
		double y0 = b[1];
		double z0 = b[2];
		return Math.sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0) + (z - z0)
				* (z - z0));
	}
	
	public static double calculateXZDistance(double[] a, double[] b)
	{
		double x = a[0];
		double z = a[1];
		
		double x0 = b[0];
		double z0 = b[1];
		return Math.sqrt((x - x0) * (x - x0) + (z - z0) * (z - z0));
	}

	@ForgeSubscribe
	public void onWorldLoad(WorldEvent.Load event) {
		if (!event.world.isRemote)
		{
			NBTTagCompound nbt = new NBTTagCompound();
			NBTSaver ds = new NBTSaver(event.world, "LambdaMod");
			nbt = ds.loadData();
	
			try {
				Integer counter = 0;
				for (Object channel : distances) {
					int distance = nbt.getInteger("channel" + counter);
					distances[counter] = nbt.getInteger("channel" + counter);
					onDistanceChanged(counter);
					counter++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void onDistanceChanged(int index) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream DOS = new DataOutputStream(bytes);
		try {
			DOS.writeInt(3); // 3 for distance update packet
			DOS.writeInt(index);
			DOS.writeInt(distances[index]);

		} catch (IOException e) {
			e.printStackTrace();
		}

		ServerPacketHandler.sendAPacket(new Packet250CustomPayload(), bytes);
	}

	@ForgeSubscribe
	public void onWorldSave(WorldEvent.Save event) {
		
		if (!event.world.isRemote)
		{
			NBTSaver ds = new NBTSaver(event.world, "LambdaMod");
			NBTTagCompound nbt = new NBTTagCompound();
			Integer counter = 0;
			for (Object channel : distances) {
				nbt.setInteger("channel" + counter, (Integer) channel);
				//System.out.println("Saved " + (Integer)channel + " to nbt");
				counter++;
			}
			ds.saveData(nbt);
		}
	}
}