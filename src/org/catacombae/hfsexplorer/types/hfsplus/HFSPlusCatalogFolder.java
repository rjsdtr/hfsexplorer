/*-
 * Copyright (C) 2006 Erik Larsson
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catacombae.hfsexplorer.types.hfsplus;

import org.catacombae.csjc.structelements.Dictionary;
import org.catacombae.hfsexplorer.types.finder.ExtendedFolderInfo;
import org.catacombae.hfsexplorer.types.finder.FolderInfo;
import org.catacombae.hfsexplorer.Util;
import java.io.PrintStream;
import java.util.Date;
import org.catacombae.csjc.StructElements;

/** This class was generated by CStructToJavaClass. */
public class HFSPlusCatalogFolder extends HFSPlusCatalogLeafRecordData implements HFSPlusCatalogAttributes, StructElements {
    /*
     * struct HFSPlusCatalogFolder
     * size: 88 bytes
     * description: 
     * 
     * BP  Size  Type                Identifier        Description
     * -----------------------------------------------------------
     * 0   2     SInt16              recordType                   
     * 2   2     UInt16              flags                        
     * 4   4     UInt32              valence                      
     * 8   4     HFSCatalogNodeID    folderID                     
     * 12  4     UInt32              createDate                   
     * 16  4     UInt32              contentModDate               
     * 20  4     UInt32              attributeModDate             
     * 24  4     UInt32              accessDate                   
     * 28  4     UInt32              backupDate                   
     * 32  16    HFSPlusBSDInfo      permissions                  
     * 48  16    FolderInfo          userInfo                     
     * 64  16    ExtendedFolderInfo  finderInfo                   
     * 80  4     UInt32              textEncoding                 
     * 84  4     UInt32              reserved                     
     */
    
    private final byte[] recordType = new byte[2];
    private final byte[] flags = new byte[2];
    private final byte[] valence = new byte[4];
    private final HFSCatalogNodeID folderID;
    private final byte[] createDate = new byte[4];
    private final byte[] contentModDate = new byte[4];
    private final byte[] attributeModDate = new byte[4];
    private final byte[] accessDate = new byte[4];
    private final byte[] backupDate = new byte[4];
    private final HFSPlusBSDInfo permissions;
    private final FolderInfo userInfo;
    private final ExtendedFolderInfo finderInfo;
    private final byte[] textEncoding = new byte[4];
    private final byte[] reserved = new byte[4];
    
    public HFSPlusCatalogFolder(byte[] data, int offset) {
	System.arraycopy(data, offset+0, recordType, 0, 2);
	System.arraycopy(data, offset+2, flags, 0, 2);
	System.arraycopy(data, offset+4, valence, 0, 4);
	folderID = new HFSCatalogNodeID(data, offset+8);
	System.arraycopy(data, offset+12, createDate, 0, 4);
	System.arraycopy(data, offset+16, contentModDate, 0, 4);
	System.arraycopy(data, offset+20, attributeModDate, 0, 4);
	System.arraycopy(data, offset+24, accessDate, 0, 4);
	System.arraycopy(data, offset+28, backupDate, 0, 4);
	permissions = new HFSPlusBSDInfo(data, offset+32);
	userInfo = new FolderInfo(data, offset+48);
	finderInfo = new ExtendedFolderInfo(data, offset+64);
	System.arraycopy(data, offset+80, textEncoding, 0, 4);
	System.arraycopy(data, offset+84, reserved, 0, 4);
    }
    
    public static int length() { return 88; }
    
    public short getRecordType() { return Util.readShortBE(recordType); }
    public short getFlags() { return Util.readShortBE(flags); }
    public int getValence() { return Util.readIntBE(valence); }
    public HFSCatalogNodeID getFolderID() { return folderID; }
    public int getCreateDate() { return Util.readIntBE(createDate); }
    public int getContentModDate() { return Util.readIntBE(contentModDate); }
    public int getAttributeModDate() { return Util.readIntBE(attributeModDate); }
    public int getAccessDate() { return Util.readIntBE(accessDate); }
    public int getBackupDate() { return Util.readIntBE(backupDate); }
    public HFSPlusBSDInfo getPermissions() { return permissions; }
    public FolderInfo getUserInfo() { return userInfo; }
    public ExtendedFolderInfo getFinderInfo() { return finderInfo; }
    public int getTextEncoding() { return Util.readIntBE(textEncoding); }
    public int getReserved() { return Util.readIntBE(reserved); }
    
    public Date getCreateDateAsDate() { return HFSPlusDate.gmtTimestampToDate(getCreateDate()); }
    public Date getContentModDateAsDate() { return HFSPlusDate.gmtTimestampToDate(getContentModDate()); }
    public Date getAttributeModDateAsDate() { return HFSPlusDate.gmtTimestampToDate(getAttributeModDate()); }
    public Date getAccessDateAsDate() { return HFSPlusDate.gmtTimestampToDate(getAccessDate()); }
    public Date getBackupDateAsDate() { return HFSPlusDate.gmtTimestampToDate(getBackupDate()); }
    
    public void printFields(PrintStream ps, String prefix) {
	ps.println(prefix + " recordType: " + getRecordType());
	ps.println(prefix + " flags: " + getFlags());
	ps.println(prefix + " valence: " + getValence());
	ps.println(prefix + " folderID: ");
	getFolderID().print(ps, prefix+"  ");
	ps.println(prefix + " createDate: " + getCreateDateAsDate());
	ps.println(prefix + " contentModDate: " + getContentModDateAsDate());
	ps.println(prefix + " attributeModDate: " + getAttributeModDateAsDate());
	ps.println(prefix + " accessDate: " + getAccessDateAsDate());
	ps.println(prefix + " backupDate: " + getBackupDateAsDate());
	ps.println(prefix + " permissions: ");
	getPermissions().print(ps, prefix+"  ");
	ps.println(prefix + " userInfo: ");
	getUserInfo().print(ps, prefix+"  ");
	ps.println(prefix + " finderInfo: ");
	getFinderInfo().print(ps, prefix+"  ");
	ps.println(prefix + " textEncoding: " + getTextEncoding());
	ps.println(prefix + " reserved: " + getReserved());
    }
    
    public void print(PrintStream ps, String prefix) {
	ps.println(prefix + "HFSPlusCatalogFolder:");
	printFields(ps, prefix);
    }

    public byte[] getBytes() {
	byte[] result = new byte[length()];
	byte[] tempData;
	int offset = 0;
        
	System.arraycopy(recordType, 0, result, offset, recordType.length); offset += recordType.length;
	System.arraycopy(flags, 0, result, offset, flags.length); offset += flags.length;
	System.arraycopy(valence, 0, result, offset, valence.length); offset += valence.length;
        tempData = folderID.getBytes();
        System.arraycopy(tempData, 0, result, offset, tempData.length); offset += tempData.length;
	System.arraycopy(createDate, 0, result, offset, createDate.length); offset += createDate.length;
	System.arraycopy(contentModDate, 0, result, offset, contentModDate.length); offset += contentModDate.length;
	System.arraycopy(attributeModDate, 0, result, offset, attributeModDate.length); offset += attributeModDate.length;
	System.arraycopy(accessDate, 0, result, offset, accessDate.length); offset += accessDate.length;
	System.arraycopy(backupDate, 0, result, offset, backupDate.length); offset += backupDate.length;
        tempData = permissions.getBytes();
        System.arraycopy(tempData, 0, result, offset, tempData.length); offset += tempData.length;
        tempData = userInfo.getBytes();
        System.arraycopy(tempData, 0, result, offset, tempData.length); offset += tempData.length;
        tempData = finderInfo.getBytes();
        System.arraycopy(tempData, 0, result, offset, tempData.length); offset += tempData.length;
        System.arraycopy(textEncoding, 0, result, offset, textEncoding.length); offset += textEncoding.length;
	System.arraycopy(reserved, 0, result, offset, reserved.length); offset += reserved.length;
        
        return result;
    }
    
    @Override
    public Dictionary getStructElements() {
        DictionaryBuilder db = new DictionaryBuilder(HFSPlusCatalogFolder.class.getSimpleName());
        
        /*
         * 0   2     SInt16              recordType                   
         * 2   2     UInt16              flags                        
         * 4   4     UInt32              valence                      
         * 8   4     HFSCatalogNodeID    folderID                     
         * 12  4     UInt32              createDate                   
         * 16  4     UInt32              contentModDate               
         * 20  4     UInt32              attributeModDate             
         * 24  4     UInt32              accessDate                   
         * 28  4     UInt32              backupDate                   
         * 32  16    HFSPlusBSDInfo      permissions                  
         * 48  16    FolderInfo          userInfo                     
         * 64  16    ExtendedFolderInfo  finderInfo                   
         * 80  4     UInt32              textEncoding                 
         * 84  4     UInt32              reserved                     
         */
        db.addUIntBE("recordType", recordType);
        db.addUIntBE("flags", flags);
        db.addUIntBE("valence", valence);
        db.add("folderID", folderID.getStructElements());
        db.add("createDate", new HFSPlusDateField(createDate, false));
        db.add("contentModDate", new HFSPlusDateField(contentModDate, false));
        db.add("attributeModDate", new HFSPlusDateField(attributeModDate, false));
        db.add("accessDate", new HFSPlusDateField(accessDate, false));
        db.add("backupDate", new HFSPlusDateField(backupDate, false));
        db.add("permissions", permissions.getStructElements());
        db.add("userInfo", userInfo.getStructElements());
        db.add("finderInfo", finderInfo.getStructElements());
        db.addUIntBE("textEncoding", textEncoding);
        db.addUIntBE("reserved", reserved);
        
        return db.getResult();
    }
}
