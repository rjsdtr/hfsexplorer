import java.util.*;
import java.io.*;
import java.math.BigInteger;

public class HFSExplorer {
    public static final String VERSION = "0.05";
    public static final String COPYRIGHT = "Copyright (C) Erik Larsson 2006";
    public static BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    private static class Options {
	public boolean readAPM = false;
	public boolean verbose = false;
    }
    private static enum Operation {
	BROWSE,
	TEST;
	
	private String filename;
	private LinkedList<String> argsList = new LinkedList<String>();
	public void addArg(String argument) {
	    argsList.add(argument);
	}
	
	public String[] getArgs() {
	    return argsList.toArray(new String[argsList.size()]);
	}
	
	public String getFilename() { return argsList.getLast(); }
    }
    
    private static Options options = new Options();
    private static Operation operation;
    private static BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
    
    public static void main(String[] args) throws IOException {
	if(args.length == 0) {
	    printUsageInfo();
	    System.exit(0);
	}
	
	parseOptions(args, 0, args.length);
	
	//RandomAccessFile isoRaf = new RandomAccessFile(args[0], "r");
	WindowsLowLevelIO isoRaf = new WindowsLowLevelIO(operation.getFilename());
	
	long offset; // Offset in isoRaf where the file system starts
	long length; // Length of the file system data
	if(options.readAPM) {
	    println("Reading the Apple Partition Map...");
	    isoRaf.seek(0x200);
	    byte[] currentBlock = new byte[512];
	    byte[] signature = new byte[2];
	    //APMPartition p = new APMPartition(isoRaf);
	    ArrayList<APMPartition> partitions = new ArrayList<APMPartition>();
	    for(int i = 0; i < 20; ++i) {
		isoRaf.readFully(currentBlock);
		signature[0] = currentBlock[0];
		signature[1] = currentBlock[1];
		if(new String(signature, "ASCII").equals("PM")) {
		    print("Partition " + i + ": ");
		    APMPartition p = new APMPartition(currentBlock, 0);
		    partitions.add(p);
		    if(options.verbose) {
			println();
			p.printPartitionInfo(System.out);
		    }
		    else
			println("\"" + p.getPmPartNameAsString() + "\" (" + p.getPmParTypeAsString() + ")");
		}
		else break;
	    }
	    print("Which partition do you wish to explore [0-" + (partitions.size()-1) + "]? ");
	    int partNum = Integer.parseInt(stdin.readLine());
	    APMPartition chosenPartition = partitions.get(partNum);
	    String partitionType = chosenPartition.getPmParTypeAsString();
	    if(!partitionType.trim().equals("Apple_HFS")) {
		println("The partition is not an HFS partition!");
		System.exit(0);
	    }
	    println("Parsing partition " + partNum + " (" + chosenPartition.getPmPartNameAsString().trim() + "/" + partitionType.trim() + ")");
	    offset = (chosenPartition.getPmPyPartStart()+chosenPartition.getPmLgDataStart())*0x200;
	    length = chosenPartition.getPmDataCnt()*0x200;
	}
	else {
	    offset = 0;
	    length = isoRaf.length();
	}
	if(operation == Operation.BROWSE)
	    operationBrowse(operation, isoRaf, offset, length);
	else if(operation == Operation.TEST) {
	    byte[] currentBlock = new byte[512];
	    isoRaf.seek(offset + 1024);
	    isoRaf.read(currentBlock);
	    HFSPlusVolumeHeader header = new HFSPlusVolumeHeader(currentBlock);
	    header.print(System.out, 2);
	    long catalogFilePosition = header.getBlockSize()*header.getCatalogFile().getExtents().getExtentDescriptor(0).getStartBlock();
	    long catalogFileLength = header.getBlockSize()*header.getCatalogFile().getExtents().getExtentDescriptor(0).getBlockCount();
	    System.out.println("Catalog file offset: " + catalogFilePosition);
	    System.out.println("Catalog file length: " + catalogFileLength + " bytes");
	    System.out.println("Seeking...");
	    isoRaf.seek(offset + catalogFilePosition);
	    System.out.println("Current file pointer: " + isoRaf.getFilePointer());
	    System.out.println("length of file: " + isoRaf.length());
	    byte[] nodeDescriptorData = new byte[14];
	    if(isoRaf.read(nodeDescriptorData) != nodeDescriptorData.length)
		System.out.println("ERROR: Did not read nodeDescriptor completely.");
	    BTNodeDescriptor btnd = new BTNodeDescriptor(nodeDescriptorData, 0);
	    btnd.print(System.out, "");
	
	    byte[] headerRec = new byte[BTHeaderRec.length()];
	    if(isoRaf.read(headerRec) != headerRec.length)
		System.out.println("ERROR: Did not read headerRec completely.");
	    BTHeaderRec bthr = new BTHeaderRec(headerRec, 0);
	    bthr.print(System.out, "");

	    // Now we have the node size, so we could just list the nodes and see what types are there.
	    // Btw, does the length of the catalog file containing this b-tree align to the node size?
	    if(catalogFileLength % bthr.getNodeSize() != 0) {
		System.out.println("catalogFileLength is not aligned to node size! (" + catalogFileLength + 
				   " % " + bthr.getNodeSize() + " = " + catalogFileLength % bthr.getNodeSize());
		return;
	    }
	    else
		System.out.println("Number of nodes in the catalog file: " + (catalogFileLength / bthr.getNodeSize()));

	    int nodeSize = bthr.getNodeSize();
	    byte[] currentNode = new byte[nodeSize];



	    // collect all records belonging to directory 1 (= ls)
	    System.out.println();
	    System.out.println();
	    HFSPlusCatalogLeafRecord[] f = collectFilesInDir(new HFSCatalogNodeID(1), bthr.getRootNode(), isoRaf, offset, header, bthr);
	    System.out.println("Found " + f.length + " items in subroot.");
	    for(HFSPlusCatalogLeafRecord rec : f) {
		//rec.print(System.out, "  ");
		System.out.print("  \"" + rec.getKey().getNodeName().getUnicodeAsString() + "\"");
		HFSPlusCatalogLeafRecordData data = rec.getData();
		if(data.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FOLDER &&
		   data instanceof HFSPlusCatalogFolder) {
		    HFSPlusCatalogFolder folderData = (HFSPlusCatalogFolder)data;
		    System.out.println(" (dir, id: " + folderData.getFolderID().toInt() + ")");
		    // Print contents of folder
		    HFSPlusCatalogLeafRecord[] f2 = collectFilesInDir(folderData.getFolderID(), bthr.getRootNode(), isoRaf, offset, header, bthr);
		    System.out.println("  Found " + f2.length + " items in " + rec.getKey().getNodeName().getUnicodeAsString() + ".");
		    for(HFSPlusCatalogLeafRecord rec2 : f2)
			System.out.println("    \"" + rec2.getKey().getNodeName() + "\"");
		    //System.out.println();
		}
		else if(data.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FILE &&
			data instanceof HFSPlusCatalogFile) {
		    HFSPlusCatalogFile fileData = (HFSPlusCatalogFile)data;
		    System.out.println(" (file, id: " + fileData.getFileID().toInt() + ")");
		}
		else if(data.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FOLDER_THREAD &&
			data instanceof HFSPlusCatalogThread) {
		    System.out.println(" (folder thread)");
		}
		else if(data.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FILE_THREAD &&
			data instanceof HFSPlusCatalogThread) {
		    System.out.println(" (file thread)");
		}
		else {
		    System.out.println(" (ENCOUNTERED UNKNOWN DATA. record type: " + data.getRecordType() + " rec: " + rec + ")");
		}
	    }
	    System.out.println();
	    System.out.println();

	
	
	    System.out.println("Reading node by node...");
	    isoRaf.seek(offset + catalogFilePosition);
	    int nodeNumber = 0;
	    int bytesRead = nodeSize;
	    while((isoRaf.getFilePointer()-catalogFilePosition+nodeSize) <= catalogFileLength) {
		//System.out.println("FP: " + isoRaf.getFilePointer() + " diff: " + (isoRaf.getFilePointer()-catalogFilePosition) + " (catalogFileLength: " + catalogFileLength + ")");
		System.out.println("Reading node " + nodeNumber + "...");
		isoRaf.readFully(currentNode);
		bytesRead += nodeSize;
	    
		BTNodeDescriptor nodeDescriptor = new BTNodeDescriptor(currentNode, 0);
		if(false && nodeDescriptor.getKind() == BTNodeDescriptor.BT_LEAF_NODE) {
		    String filename = "node" + nodeNumber + ".bin";
		    System.out.println("Dumping node to file: \"" + filename + "\"");
		    FileOutputStream fos = new FileOutputStream(filename);
		    fos.write(currentNode);
		    fos.close();
		}
		System.out.println("  Kind: " + nodeDescriptor.getKindAsString());
		System.out.println("  Number of records: " + nodeDescriptor.getNumRecords());
		short[] offsets = new short[nodeDescriptor.getNumRecords()];
		for(int i = 0; i < offsets.length; ++i) {
		    offsets[i] = Util.readShortBE(currentNode, currentNode.length-((i+1)*2));
		}
	    
		for(int i = 0; i < offsets.length; ++i) {
		    int currentOffset = Util2.unsign(offsets[i]);

		    if(i < offsets.length-1) {
		    //System.out.println("  [" + nodeNumber + "] Offset to record " + i + ": " + currentOffset);
		    //System.out.println("  [" + nodeNumber + "]  Size of record: " + (Util2.unsign(offsets[i+1])-currentOffset) + " bytes");
		    /*
		    int keyLength;
		    int keyLengthSize;
		    if(bthr.isBTBigKeysSet()) {
			keyLength = Util2.unsign(Util.readShortBE(currentNode, currentOffset));
			keyLengthSize = 2;
		    }
		    else {
			keyLength = Util2.unsign(Util.readByteBE(currentNode, currentOffset));
			keyLengthSize = 1;
		    }
		    */
			if(nodeDescriptor.getKind() != BTNodeDescriptor.BT_HEADER_NODE) {
			    HFSPlusCatalogKey currentKey = new HFSPlusCatalogKey(currentNode, currentOffset);
			    
			    
			    if(nodeDescriptor.getKind() == BTNodeDescriptor.BT_LEAF_NODE) {
				System.out.println("  [" + nodeNumber + "]  Key: " + currentKey.getKeyLength() + 
						   ", " + currentKey.getParentID().toString() +
						   ", \"" + currentKey.getNodeName().getUnicodeAsString() + "\"");
				//currentKey.print(System.out, "  [" + nodeNumber + "]   ");
				
				short recordType = Util.readShortBE(currentNode, currentOffset+currentKey.length());
				System.out.print("  [" + nodeNumber + "]   Record type: ");
				if(recordType == 0x0001) System.out.print("kHFSPlusFolderRecord");
				else if(recordType == 0x0002) System.out.print("kHFSPlusFileRecord");
				else if(recordType == 0x0003) System.out.print("kHFSPlusFolderThreadRecord");
				else if(recordType == 0x0004) System.out.print("kHFSPlusFileThreadRecord");
				else System.out.print("UNKNOWN! (" + recordType + ")");
				System.out.println();
				
				//System.out.println("  [" + nodeNumber + "]  Record:");
				if(recordType == 0x0001) {
				    HFSPlusCatalogFolder folderRec = new HFSPlusCatalogFolder(currentNode, currentOffset+currentKey.length());
				    System.out.println("  [" + nodeNumber + "]   Node ID: " + folderRec.getFolderID());
				    System.out.println("  [" + nodeNumber + "]   Valence: " + folderRec.getValence());
				    //folderRec.print(System.out, "  [" + nodeNumber + "]   ");
				}
				else if(recordType == 0x0002) {
				    HFSPlusCatalogFile fileRec = new HFSPlusCatalogFile(currentNode, currentOffset+currentKey.length());
				    System.out.println("  [" + nodeNumber + "]   Node ID: " + fileRec.getFileID());
				    //fileRec.print(System.out, "  [" + nodeNumber + "]   ");			    
				}
				else if(recordType == 0x0003) {
				    HFSPlusCatalogThread folderThreadRec = new HFSPlusCatalogThread(currentNode, currentOffset+currentKey.length());
				    //folderThreadRec.print(System.out, "  [" + nodeNumber + "]   ");
				}
				else if(recordType == 0x0004) {
				    HFSPlusCatalogThread fileThreadRec = new HFSPlusCatalogThread(currentNode, currentOffset+currentKey.length());
				    //fileThreadRec.print(System.out, "  [" + nodeNumber + "]   ");
				}
			    }
			    else if(nodeDescriptor.getKind() == BTNodeDescriptor.BT_INDEX_NODE) {
// 			    System.out.println("  [" + nodeNumber + "]  Remaining data for index: " + 
// 					       ((Util2.unsign(offsets[i+1])-currentOffset)-
// 						Util2.unsign(currentKey.length())) + " bytes");
				System.out.println("  [" + nodeNumber + "]    \"" + currentKey.getNodeName().getUnicodeAsString() + "\" (parent: " + currentKey.getParentID() + ") -> " + Util2.unsign(Util.readIntBE(currentNode, currentOffset+currentKey.length())));
			    }
			}
		    }
		    else {
// 		    System.out.println("  [" + nodeNumber + "] Offset to free space: " + currentOffset);
// 		    System.out.println("  [" + nodeNumber + "]  Size of free space: " + (nodeSize-currentOffset-2*nodeDescriptor.getNumRecords()) + " bytes");
		    
		    }
		}
		
		if(true) {
		    System.out.print("Press enter to read next node (q and enter to exit)...");
		    if(stdIn.readLine().trim().equalsIgnoreCase("q"))
			return;
		}
		++nodeNumber;
	    }
	    System.out.println("FP: " + isoRaf.getFilePointer() + " diff: " + (isoRaf.getFilePointer()-catalogFilePosition) + " (catalogFileLength: " + catalogFileLength + ")");
	    System.out.println("bytesRead: " + bytesRead + " nodeSize: " + nodeSize + " number of nodes: " + (catalogFileLength / nodeSize));
	    System.out.println("Nodes read: " + nodeNumber);
	}
    }
    
    public static void operationBrowse(Operation op, LowLevelFile hfsFile, long fsOffset, long fsLength) {
	HFSFileSystemView fsView = new HFSFileSystemView(hfsFile, fsOffset);
	HFSPlusCatalogLeafRecord rootRecord = fsView.getRoot();
	HFSPlusCatalogLeafRecord currentDir = rootRecord;
	LinkedList<String> pathStack = new LinkedList<String>();
	pathStack.addLast("");

	BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
	
	while(true) {
	    StringBuilder currentPath = new StringBuilder();
	    for(String pathComponent : pathStack) {
		currentPath.append(pathComponent);
		currentPath.append("/");
	    }
	    println("Listing files in \"" + currentPath.toString() + "\":");
	    
	    HFSPlusCatalogLeafRecord[] recordsInDir = fsView.listRecords(currentDir);
	    for(HFSPlusCatalogLeafRecord rec : recordsInDir) {
		HFSPlusCatalogLeafRecordData recData = rec.getData();
		if(recData.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FILE &&
		   recData instanceof HFSPlusCatalogFile) {
		    HFSPlusCatalogFile catFile = (HFSPlusCatalogFile)recData;
		    println("  [" + catFile.getFileID() + "] \"" + rec.getKey().getNodeName() + "\" (" + catFile.getDataFork().getLogicalSize() + " B)");
		}
		else if(recData.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FOLDER &&
			recData instanceof HFSPlusCatalogFolder) {
		    HFSPlusCatalogFolder catFolder = (HFSPlusCatalogFolder)recData;
		    println("  [" + catFolder.getFolderID() + "] \"" + rec.getKey().getNodeName() + "/\"");
		}
	    }
	    
	    //long nextID = -1;
	    while(true) {
		print("Command[?]: ");
		
		String input = null;
		try {
		    input = stdIn.readLine().trim();
		} catch(IOException ioe) {
		    ioe.printStackTrace();
		    return;
		}
		if(input.equalsIgnoreCase("?")) {
		    println("Available commands:");
		    println(" ls                List contents of current directory");
		    println(" cd <dirName>      Changes directory by name");
		    println(" cdn <dirID>       Changes directory by ID");
		    println(" info <fileID>     Gets extensive information about the file.");
		    println(" extract <fileID>  Extracts <fileID> to current directory");
		    println(" q                 Quits program");
		}
		else if(input.equals("q"))
		    return;
		else if(input.equals("ls"))
		    break;
		else if(input.startsWith("extract ")) {
		    input = input.substring("extract ".length()).trim();
		    try {
			long nextID = Long.parseLong(input);
			
			HFSPlusCatalogLeafRecord selectedFileRecord = null;
			HFSPlusCatalogFile selectedFile = null;
			for(HFSPlusCatalogLeafRecord rec : recordsInDir) {
			    HFSPlusCatalogLeafRecordData recData = rec.getData();
			    if(recData.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FILE &&
			       recData instanceof HFSPlusCatalogFile) {
				HFSPlusCatalogFile catFile = (HFSPlusCatalogFile)recData;
				if(Util2.unsign(catFile.getFileID().toInt()) == nextID) {
				    selectedFileRecord = rec;
				    selectedFile = (HFSPlusCatalogFile)recData;
				    break;
				}
			    }
			}
			if(selectedFileRecord == null) {
			    println("ID not present in dir.");
			    //nextID = -1;
			}
			else {
			    String dataForkFilename = selectedFileRecord.getKey().getNodeName().toString();
			    FileOutputStream dataOut = new FileOutputStream(dataForkFilename);
			    print("Extracting data fork to file \"" + dataForkFilename + "\"...");
			    try {
				long bytesExtracted = fsView.extractDataForkToStream(selectedFile, dataOut);
				println("extracted " + bytesExtracted + " bytes.");
				dataOut.close();
			    } catch(IOException ioe) {
				ioe.printStackTrace();
				try { dataOut.close(); } catch(IOException ioe2) {}
				continue; // or rather... don't continue (: mwahaha
			    }
			    
			    String resourceForkFilename = dataForkFilename + ".resourcefork";
			    FileOutputStream resourceOut = new FileOutputStream(resourceForkFilename);
			    print("Extracting resource fork to file \"" + resourceForkFilename + "\"...");
			    try {
				long bytesExtracted = fsView.extractResourceForkToStream(selectedFile, resourceOut);
				println("extracted " + bytesExtracted + " bytes.");
				resourceOut.close();
			    } catch(IOException ioe) {
				ioe.printStackTrace();
				try { dataOut.close(); } catch(IOException ioe2) {}
			    }
			    //break; // to reread the directory
			}
			
		    } catch(FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		    } catch(NumberFormatException nfe) {
			//nextID = -1;
			println("Invalid input!");
		    }
		}
		else if(input.startsWith("info ")) {
		    input = input.substring("info ".length()).trim();
		    try {
			long nextID = Long.parseLong(input);
			
			HFSPlusCatalogLeafRecord selectedFileRecord = null;
			for(HFSPlusCatalogLeafRecord rec : recordsInDir) {
			    HFSPlusCatalogLeafRecordData recData = rec.getData();
			    if(recData.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FILE &&
			       recData instanceof HFSPlusCatalogFile) {
				HFSPlusCatalogFile catFile = (HFSPlusCatalogFile)recData;
				if(Util2.unsign(catFile.getFileID().toInt()) == nextID) {
				    selectedFileRecord = rec;
				    rec.print(System.out, "");
				    break;
				}
			    }
			}
			if(selectedFileRecord == null) {
			    println("ID not present in dir.");
			    //nextID = -1;
			}
		    } catch(NumberFormatException nfe) {
			//nextID = -1;
			println("Invalid input!");
		    }
		}
		else if(input.startsWith("cdn ")) {
		    input = input.substring("cdn ".length()).trim();
		    if(input.equals("..")) {
			println("Not yet implemented.");
			// Implement this.
		    }
		    else {
			try {
			    long nextID = Long.parseLong(input);
			    HFSPlusCatalogLeafRecord nextDir = null;
			    for(HFSPlusCatalogLeafRecord rec : recordsInDir) {
				HFSPlusCatalogLeafRecordData recData = rec.getData();
				if(recData.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FOLDER &&
				   recData instanceof HFSPlusCatalogFolder) {
				    HFSPlusCatalogFolder catFolder = (HFSPlusCatalogFolder)recData;
				    if(Util2.unsign(catFolder.getFolderID().toInt()) == nextID) {
					nextDir = rec;
					break;
				    }
				}
			    }
			    if(nextDir == null) {
				println("ID not present in dir.");
				//nextID = -1;
			    }
			    else {
				pathStack.addLast(nextDir.getKey().getNodeName().toString());
				currentDir = nextDir;
				break;
			    }
			} catch(Exception e) {
			    //nextID = -1;
			    println("Invalid input!");
			}
		    }
		}
		else if(input.startsWith("cd ")) {
		    input = input.substring("cd ".length());
		    if(input.equals("..")) {
			println("Not yet implemented.");
			// Implement this.
		    }
		    else {
			try {
			    
			    HFSPlusCatalogLeafRecord nextDir = null;
			    for(HFSPlusCatalogLeafRecord rec : recordsInDir) {
				HFSPlusCatalogLeafRecordData recData = rec.getData();
				if(recData.getRecordType() == HFSPlusCatalogLeafRecordData.RECORD_TYPE_FOLDER &&
				   recData instanceof HFSPlusCatalogFolder) {
				    //System.out.println(rec.getKey().getNodeName() + ".equals(" + input +")");
				    if(rec.getKey().getNodeName().toString().equals(input)) {
					nextDir = rec;
					break;
				    }
				}
			    }
			    if(nextDir == null) {
				println("Unknown directory.");
				//nextID = -1;
			    }
			    else {
				pathStack.addLast(nextDir.getKey().getNodeName().toString());
				currentDir = nextDir;
				break;
			    }
			} catch(Exception e) {
			    //nextID = -1;
			    println("Invalid input!");
			}
		    }
		}
		else
		    println("Unknown command.");
	    }
	    println();
	}
    }
    public static void printUsageInfo() {
	// For measurment of the standard terminal width in fixed width environments:
	// 79:  <------------------------------------------------------------------------------->
	println("HFSExplorer v" + VERSION);
	println(COPYRIGHT);
	println("  displays information about an HFS filesystem.");
	println("  usage: java HFSExplorer [common options] <verb> [verb options] <file/device>");
	println();
	println("  Common options:");
	println("    -apm  Specifies that the HFS partition is embedded within an Apple");
	println("          Partition Map. The user will be allowed to choose which partition in");
	println("          the map to attempt reading.");
	println("    -v    Verbose operation.");
	println();
	println("  Verbs:");
	println("    browse  Launches a mode where the user can browse the files in a HFS+ file");
	println("            system.");
	println("    test    Launches a test mode for extensive exploration of file system");
	println("            structures. Only for debugging purposes.");
	println();
	println("  Verbs options:");
	println("    <none defined>");
    }
    
    public static void println() {
	//System.out.print(BACKSPACE79);
	System.out.println();
    }
    public static void println(String s) {
	//System.out.print(BACKSPACE79);
	System.out.println(s);
    }
    public static void print(String s) {
	//System.out.print(BACKSPACE79);
	System.out.print(s);
    }
    public static void vprintln() {
	//System.out.print(BACKSPACE79);
	if(options.verbose) System.out.println();
    }
    public static void vprintln(String s) {
	//System.out.print(BACKSPACE79);
	if(options.verbose) System.out.println(s);
    }
    public static void vprint(String s) {
	//System.out.print(BACKSPACE79);
	if(options.verbose) System.out.print(s);
    }
    
    public static void parseOptions(String[] arguments, int offset, int length) {
	int i;
	String currentArg = null;
	for(i = offset; i < length; ++i) {
	    currentArg = arguments[i];
	    if(!currentArg.startsWith("-"))
		break;
	    else if(currentArg.equals("-apm"))
		options.readAPM = true;
	    else if(currentArg.equals("-v"))
		options.verbose = true;
	    else
		println("\"" + currentArg + "\" is not a valid parameter.");
	}
	// Now comes the verb
	if(currentArg.equals("browse")) {
	    operation = Operation.BROWSE;
	}
	if(currentArg.equals("test")) {
	    operation = Operation.TEST;
	}
	for(++i; i < length; ++i)
	    operation.addArg(arguments[i]);
	
	//System.out.println("SETTING FILENAME TO!! ::: " + arguments[length-1]);
	//operation.setFilename(arguments[length-1]);
    }
    
    public static HFSPlusCatalogLeafRecord[] collectFilesInDir(HFSCatalogNodeID dirID, int currentNodeNumber, WindowsLowLevelIO isoRaf, long offset, HFSPlusVolumeHeader header, BTHeaderRec bthr) {
	// Try to list contents in specified dir
	int requestedDir = dirID.toInt();
	long catalogFilePosition = header.getBlockSize()*header.getCatalogFile().getExtents().getExtentDescriptor(0).getStartBlock();
	
	byte[] currentNodeData = new byte[bthr.getNodeSize()];
	isoRaf.seek(offset + catalogFilePosition + Util2.unsign(currentNodeNumber)*bthr.getNodeSize());
	isoRaf.readFully(currentNodeData);
	
	BTNodeDescriptor nodeDescriptor = new BTNodeDescriptor(currentNodeData, 0);
	if(nodeDescriptor.getKind() == BTNodeDescriptor.BT_INDEX_NODE) {
	    BTIndexNode currentNode = new HFSPlusCatalogIndexNode(currentNodeData, 0, bthr.getNodeSize());
	    BTIndexRecord[] matchingRecords = findLEChildKeys(currentNode, dirID);
	    //System.out.println("Matching records: " + matchingRecords.length);
	    
	    LinkedList<HFSPlusCatalogLeafRecord> results = new LinkedList<HFSPlusCatalogLeafRecord>();
	    for(BTIndexRecord bir : matchingRecords) {
		HFSPlusCatalogLeafRecord[] partResult = collectFilesInDir(dirID, bir.getIndex(), isoRaf, 
									  offset, header, bthr);
		for(HFSPlusCatalogLeafRecord curRes : partResult)
		    results.addLast(curRes);
	    }
	    return results.toArray(new HFSPlusCatalogLeafRecord[results.size()]);
	}
	else if(nodeDescriptor.getKind() == BTNodeDescriptor.BT_LEAF_NODE) {
	    HFSPlusCatalogLeafNode currentNode = new HFSPlusCatalogLeafNode(currentNodeData, 0, Util2.unsign(bthr.getNodeSize()));
	    return getChildrenTo(currentNode, dirID);
	}
	else
	    throw new RuntimeException("Illegal type for node!");
    }
    
    public static BTIndexRecord findLEKey(BTIndexNode indexNode, int nodeID, String searchString) {
	/* 
	 * Algoritm:
	 *   Key searchKey
	 *   Key greatestMatchingKey;
	 *   For each n : records
	 *     If n.key <= searchKey && n.key > greatestMatchingKey
	 *       greatestMatchingKey = n.key
	 */
	HFSPlusCatalogKey searchKey = new HFSPlusCatalogKey(nodeID, searchString);
	long unsignedNodeID = Util2.unsign(nodeID);
	BTIndexRecord records[] = indexNode.getIndexRecords();
	BTIndexRecord largestMatchingRecord = null;//records[0];
	for(int i = 0; i < records.length; ++i) {
	    if(records[i].getKey().compareTo(searchKey) <= 0 && 
	       (largestMatchingRecord == null || records[i].getKey().compareTo(largestMatchingRecord.getKey()) > 0)) {
		largestMatchingRecord = records[i];
	    }
	}
	return largestMatchingRecord;
    }
    
    public static BTIndexRecord[] findLEChildKeys(BTIndexNode indexNode, HFSCatalogNodeID rootFolderID) {
	LinkedList<BTIndexRecord> result = new LinkedList<BTIndexRecord>();
	BTIndexRecord records[] = indexNode.getIndexRecords();
	BTIndexRecord largestMatchingRecord = null;//records[0];
	HFSPlusCatalogKey largestMatchingKey = null;
	for(int i = 0; i < records.length; ++i) {
	    if(records[i].getKey() instanceof HFSPlusCatalogKey) {
		HFSPlusCatalogKey key = (HFSPlusCatalogKey)records[i].getKey();
		if(key.getParentID().toInt() < rootFolderID.toInt() && 
		   (largestMatchingKey == null || 
		    key.getParentID().toInt() > largestMatchingKey.getParentID().toInt())) {
		    largestMatchingKey = key;
		    largestMatchingRecord = records[i];
		}
		else if(key.getParentID().toInt() == rootFolderID.toInt())
		    result.addLast(records[i]);
	    }
	    else
		System.out.println("UNKNOWN KEY TYPE IN findLEChildKeys");
	}
	
	if(largestMatchingKey != null)
	    result.addFirst(largestMatchingRecord);
	return result.toArray(new BTIndexRecord[result.size()]);
	
	/*
	  if exists in index node a value equal to the nodeID
	 */
    }
    
    public static HFSPlusCatalogLeafRecord[] getChildrenTo(HFSPlusCatalogLeafNode leafNode, HFSCatalogNodeID nodeID) {
	LinkedList<HFSPlusCatalogLeafRecord> children = new LinkedList<HFSPlusCatalogLeafRecord>();
	HFSPlusCatalogLeafRecord[] records = leafNode.getLeafRecords();
	for(int i = 0; i < records.length; ++i) {
	    HFSPlusCatalogLeafRecord curRec = records[i];
	    if(curRec.getKey().getParentID().toInt() == nodeID.toInt())
		children.addLast(curRec);
	}
	return children.toArray(new HFSPlusCatalogLeafRecord[children.size()]);
    }

    public static HFSPlusCatalogFile findFileID(HFSPlusCatalogLeafNode leafNode, HFSCatalogNodeID nodeID) {
	HFSPlusCatalogLeafRecord[] records = leafNode.getLeafRecords();
	for(int i = 0; i < records.length; ++i) {
	    HFSPlusCatalogLeafRecord curRec = records[i];
	    HFSPlusCatalogLeafRecordData curRecData = curRec.getData();
	    if(curRecData instanceof HFSPlusCatalogFile && 
	       ((HFSPlusCatalogFile)curRecData).getFileID().toInt() == nodeID.toInt()) {
		return (HFSPlusCatalogFile)curRecData;
	    }
	}
	return null;
    }
    public static HFSPlusCatalogFolder findFolderID(HFSPlusCatalogLeafNode leafNode, HFSCatalogNodeID nodeID) {
	HFSPlusCatalogLeafRecord[] records = leafNode.getLeafRecords();
	for(int i = 0; i < records.length; ++i) {
	    HFSPlusCatalogLeafRecord curRec = records[i];
	    HFSPlusCatalogLeafRecordData curRecData = curRec.getData();
	    if(curRecData instanceof HFSPlusCatalogFolder && 
	       ((HFSPlusCatalogFolder)curRecData).getFolderID().toInt() == nodeID.toInt()) {
		return (HFSPlusCatalogFolder)curRecData;
	    }
	}
	return null;
    }
}
