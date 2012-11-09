/*-
 * Copyright (C) 2011-2012 Erik Larsson
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

package org.catacombae.hfs.types.hfsplus;

import java.io.PrintStream;
import org.catacombae.csjc.DynamicStruct;
import org.catacombae.csjc.PrintableStruct;
import org.catacombae.util.Util;

/** This class was generated by CStructToJavaClass. */
public class BlockListHeader implements DynamicStruct, PrintableStruct {
    /*
     * struct BlockListHeader
     * size: 32 bytes
     * description:
     *
     * BP  Size  Type          Identifier  Description
     * -----------------------------------------------
     * 0   2     UInt16        maxBlocks
     * 2   2     UInt16        numBlocks
     * 4   4     UInt32        bytesUsed
     * 8   4     UInt32        checksum
     * 12  4     UInt32        pad
     * 16  16*1  BlockInfo[1]  binfo
     */

    public static final int STRUCTSIZE = 32;

    private final boolean littleEndian;

    private short maxBlocks;
    private short numBlocks;
    private int bytesUsed;
    private int checksum;
    private int pad;
    private final BlockInfo[] binfo;

    public BlockListHeader(byte[] data, int offset, boolean littleEndian) {
        this.littleEndian = littleEndian;

        if(!littleEndian) {
            this.maxBlocks = Util.readShortBE(data, offset+0);
            this.numBlocks = Util.readShortBE(data, offset+2);
            this.bytesUsed = Util.readIntBE(data, offset+4);
            this.checksum = Util.readIntBE(data, offset+8);
            this.pad = Util.readIntBE(data, offset+12);
        }
        else {
            this.maxBlocks = Util.readShortLE(data, offset+0);
            this.numBlocks = Util.readShortLE(data, offset+2);
            this.bytesUsed = Util.readIntLE(data, offset+4);
            this.checksum = Util.readIntLE(data, offset+8);
            this.pad = Util.readIntLE(data, offset+12);
        }

        this.binfo = new BlockInfo[Util.unsign(this.numBlocks) + 1];
        for(int i = 0; i < binfo.length; ++i) {
            this.binfo[i] = new BlockInfo(data,
                    offset+16 + i*BlockInfo.length(), littleEndian);
        }
    }

    public int maxSize() {
        return STRUCTSIZE + 65535 * BlockInfo.length();
    }

    public int occupiedSize() {
        return STRUCTSIZE + (this.binfo.length - 1) * BlockInfo.length();
    }

    public final boolean isLittleEndian() { return littleEndian; }

    /**  */
    public final int getMaxBlocks() { return Util.unsign(getRawMaxBlocks()); }
    /**  */
    public final int getNumBlocks() { return Util.unsign(getRawNumBlocks()); }
    /**  */
    public final long getBytesUsed() { return Util.unsign(getRawBytesUsed()); }
    /**  */
    public final long getChecksum() { return Util.unsign(getRawChecksum()); }
    /**  */
    public final long getPad() { return Util.unsign(getRawPad()); }
    /**  */
    public final BlockInfo getBinfo(int index) {
        return (index >= binfo.length || index < 0) ? null : binfo[index];
    }

    /** <b>Note that the return value from this function should be interpreted as an unsigned integer, for instance using Util.unsign(...).</b> */
    public final short getRawMaxBlocks() { return maxBlocks; }
    /** <b>Note that the return value from this function should be interpreted as an unsigned integer, for instance using Util.unsign(...).</b> */
    public final short getRawNumBlocks() { return numBlocks; }
    /** <b>Note that the return value from this function should be interpreted as an unsigned integer, for instance using Util.unsign(...).</b> */
    public final int getRawBytesUsed() { return bytesUsed; }
    /** <b>Note that the return value from this function should be interpreted as an unsigned integer, for instance using Util.unsign(...).</b> */
    public final int getRawChecksum() { return checksum; }
    /** <b>Note that the return value from this function should be interpreted as an unsigned integer, for instance using Util.unsign(...).</b> */
    public final int getRawPad() { return pad; }

    public void printFields(PrintStream ps, String prefix) {
        ps.println(prefix + " maxBlocks: " + getMaxBlocks());
        ps.println(prefix + " numBlocks: " + getNumBlocks());
        ps.println(prefix + " bytesUsed: " + getBytesUsed());
        ps.println(prefix + " checksum: " + getChecksum());
        ps.println(prefix + " pad: " + getPad());
        ps.println(prefix + " binfo: ");
        for(int i = 0; i < binfo.length; ++i) {
            ps.println(prefix + "  [" + i + "]: ");
            binfo[i].print(ps, prefix + "   ");
        }
    }

    public void print(PrintStream ps, String prefix) {
        ps.println(prefix + "BlockListHeader:");
        printFields(ps, prefix);
    }

    public byte[] getBytes() {
        byte[] result = new byte[occupiedSize()];
        int offset = 0;

        if(!littleEndian) {
            Util.arrayPutBE(result, offset, maxBlocks); offset += 2;
            Util.arrayPutBE(result, offset, numBlocks); offset += 2;
            Util.arrayPutBE(result, offset, bytesUsed); offset += 4;
            Util.arrayPutBE(result, offset, checksum); offset += 4;
            Util.arrayPutBE(result, offset, pad); offset += 4;
        }
        else {
            Util.arrayPutLE(result, offset, maxBlocks); offset += 2;
            Util.arrayPutLE(result, offset, numBlocks); offset += 2;
            Util.arrayPutLE(result, offset, bytesUsed); offset += 4;
            Util.arrayPutLE(result, offset, checksum); offset += 4;
            Util.arrayPutLE(result, offset, pad); offset += 4;
        }

        for(int i = 0; i < this.binfo.length; ++i) {
            byte[] tempData = this.binfo[i].getBytes();
            System.arraycopy(tempData, 0, result, offset, tempData.length); offset += tempData.length;
        }
        return result;
    }

    public int calculateChecksum() {
        byte[] data = getBytes();
        int cksum = 0;

        /* Set checksum field to 0 before checksumming. */
        data[8] = 0;
        data[9] = 0;
        data[10] = 0;
        data[11] = 0;

        for(int i = 0; i < data.length; i++) {
            cksum = (cksum << 8) ^ (cksum + Util.unsign(data[i]));
        }

        return (~cksum);
    }
}
