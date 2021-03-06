package entanglecraft;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class NBTSaver {
	private World w;
	private String fileName;
	public NBTSaver(World world, String datafile){
		w = world;
		fileName = datafile;		
	}
	
	public static void writeFieldToNBT(NBTTagCompound nbt, String field, Object o, String name) {
		if (field == "int[]") 
		{
			if (o != null)
			{
				int[] theArray = (int[]) o;
				if (theArray != null) 
				{
					nbt.setIntArray(name, theArray);
				}
			}
		}
	}
	
	public File getWorldSaveDir(World world){
		File workingDir = EntangleCraft.proxy.getWorldSaveDir(world);
		return workingDir;
	}
	
	public NBTTagCompound loadData(){
		NBTTagCompound data = new NBTTagCompound();
		File saveDir = getWorldSaveDir(w);
		if (!saveDir.exists())
			saveDir.mkdir();
		File file = new File(saveDir,fileName + ".dat");
		try {
			DataInputStream DIS = new DataInputStream(new FileInputStream(file));
			data = CompressedStreamTools.read(DIS);
		} catch (IOException e) {
			data = new NBTTagCompound();
		}
		return data;
	}
	
	public void saveData(NBTTagCompound nbt){
		File saveDir = getWorldSaveDir(w);
		if (!saveDir.exists())
			saveDir.mkdir();
		File file = new File(saveDir,fileName + ".dat");
		if (!file.exists());
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			DataOutputStream DOS = new DataOutputStream(new FileOutputStream(file));
			CompressedStreamTools.write(nbt, DOS);
		} catch (IOException e){ 
			e.printStackTrace();
		}
	}
}
