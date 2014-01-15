/*
 * Copyright (C) 2012-2014 The Android Open Source Project 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.android.jobb;

import Twofish.Twofish_Algorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.util.Arrays;

public class ObbFile {
    public static final int OBB_OVERLAY = (1 << 0);
    public static final int OBB_SALTED = (1 << 1);

    static final int kFooterTagSize = 8; /* last two 32-bit integers */

    static final int kFooterMinSize = 33; /*
                                           * 32-bit signature version (4 bytes)
                                           * 32-bit package version (4 bytes)
                                           * 32-bit flags (4 bytes) 64-bit salt
                                           * (8 bytes) 32-bit package name size
                                           * (4 bytes) >=1-character package
                                           * name (1 byte) 32-bit footer size (4
                                           * bytes) 32-bit footer marker (4
                                           * bytes)
                                           */

    static final int kMaxBufSize = 32768; /* Maximum file read buffer */

    static final long kSignature = 0x01059983; /* ObbFile signature */

    static final int kSigVersion = 1; /* We only know about signature version 1 */

    /* offsets in version 1 of the header */
    static final int kPackageVersionOffset = 4;
    static final int kFlagsOffset = 8;
    static final int kSaltOffset = 12;
    static final int kPackageNameLenOffset = 20;
    static final int kPackageNameOffset = 24;

    long mPackageVersion = -1, mFlags;
    String mPackageName;
    byte[] mSalt = new byte[8];
    long mFileSystemSize;

    public ObbFile() {
    }

    public boolean readFrom(String filename)
    {
        File obbFile = new File(filename);
        return readFrom(obbFile);
    }

    public boolean readFrom(File obbFile)
    {
        return parseObbFile(obbFile);
    }

    static public long get4LE(ByteBuffer buf) {
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return (buf.getInt() & 0xFFFFFFFFL);
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public void setSalt(byte[] salt) {
        if (salt.length != mSalt.length) {
            throw new RuntimeException("salt must be " + mSalt.length + " characters in length");
        }
        System.arraycopy(salt, 0, mSalt, 0, mSalt.length);
    }

    public void setPackageVersion(long packageVersion) {
        mPackageVersion = packageVersion;
    }

    public void setFlags(long flags) {
        mFlags = flags;
    }

    public boolean parseObbFile(File obbFile)
    {
        try {
            long fileLength = obbFile.length();

            if (fileLength < kFooterMinSize) {
                throw new RuntimeException("file is only " + fileLength + " (less than "
                        + kFooterMinSize + " minimum)");
            }

            RandomAccessFile raf = new RandomAccessFile(obbFile, "r");
            raf.seek(fileLength - kFooterTagSize);
            byte[] footer = new byte[kFooterTagSize];
            raf.readFully(footer);
            ByteBuffer footBuf = ByteBuffer.wrap(footer);
            footBuf.position(4);
            long fileSig = get4LE(footBuf);
            if (fileSig != kSignature) {
                throw new RuntimeException("footer didn't match magic string (expected 0x"
                        + Long.toHexString(kSignature) + ";got 0x" +
                        Long.toHexString(fileSig) + ")");
            }

            footBuf.rewind();
            long footerSize = get4LE(footBuf);
            if (footerSize > fileLength - kFooterTagSize
                    || footerSize > kMaxBufSize) {
                throw new RuntimeException("claimed footer size is too large (0x"
                        + Long.toHexString(footerSize) + "; file size is 0x" +
                        Long.toHexString(fileLength) + ")");
            }

            if (footerSize < (kFooterMinSize - kFooterTagSize)) {
                throw new RuntimeException("claimed footer size is too small (0x"
                        + Long.toHexString(footerSize) + "; minimum size is 0x" +
                        Long.toHexString(kFooterMinSize - kFooterTagSize));
            }

            long fileOffset = fileLength - footerSize - kFooterTagSize;
            mFileSystemSize = fileOffset;
            raf.seek(fileOffset);

            footer = new byte[(int) footerSize];
            raf.readFully(footer);
            footBuf = ByteBuffer.wrap(footer);

            long sigVersion = get4LE(footBuf);
            if (sigVersion != kSigVersion) {
                throw new RuntimeException("Unsupported ObbFile version " + sigVersion);
            }

            footBuf.position(kPackageVersionOffset);
            mPackageVersion = get4LE(footBuf);
            footBuf.position(kFlagsOffset);
            mFlags = get4LE(footBuf);

            footBuf.position(kSaltOffset);
            footBuf.get(mSalt);
            footBuf.position(kPackageNameLenOffset);
            long packageNameLen = get4LE(footBuf);
            if (packageNameLen == 0
                    || packageNameLen > (footerSize - kPackageNameOffset)) {
                throw new RuntimeException(
                        "bad ObbFile package name length (0x" + Long.toHexString(packageNameLen) +
                                "; 0x" + Long.toHexString(footerSize - kPackageNameOffset)
                                + "possible)");
            }
            byte[] packageNameBuf = new byte[(int) packageNameLen];
            footBuf.position(kPackageNameOffset);
            footBuf.get(packageNameBuf);

            mPackageName = new String(packageNameBuf);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public final static int BYTES_PER_SECTOR = 512;

    /**
     * plain: the initial vector is the 32-bit little-endian version of the
     * sector number, padded with zeros if necessary.
     */
    static private void cryptIVPlainGen(int sector, byte[] out) {
        Arrays.fill(out, (byte) 0);
        out[0] = (byte) (sector & 0xff);
        out[1] = (byte) (sector >> 8 & 0xff);
        out[2] = (byte) (sector >> 16 & 0xff);
        out[3] = (byte) (sector >>> 24);
    }

    final private static int sBlockSize = Twofish_Algorithm.blockSize();
    static byte[] sBufLast = new byte[sBlockSize];
    final private static int sNumSectorBlocks = BYTES_PER_SECTOR / sBlockSize;
    static byte[] sTempSector = new byte[BYTES_PER_SECTOR];

    static private void encryptSector(int sector, byte[] sectorBuf, Object key) {
        // set initialization vector
        cryptIVPlainGen(sector, sBufLast);

        int pos = 0;
        for (int i = 0; i < sNumSectorBlocks; i++) {
            // encrypt with chained blocks --- xor with the previous encrypted
            // block
            int blockEnd = sBlockSize + pos;
            for (int j = 0; j < sBlockSize; j++) {
                sectorBuf[j + pos] ^= sBufLast[j];
            }
            byte[] encryptBuf = Twofish_Algorithm.blockEncrypt(sectorBuf, pos, key);
            sBufLast = encryptBuf;
            // encrypt in place
            System.arraycopy(encryptBuf, 0, sectorBuf, pos, sBlockSize);
            pos += sBlockSize;
        }
    }

    static private void decryptSector(int sector, byte[] sectorBuf, Object key) {

        // set initialization vector
        cryptIVPlainGen(sector, sBufLast);

        int pos = 0;
        for (int i = 0; i < sNumSectorBlocks; i++) {
            // decrypt with chained blocks --- xor with the previous encrypted
            // block
            byte[] decryptBuf = Twofish_Algorithm.blockDecrypt(sectorBuf, pos, key);
            for (int j = 0; j < sBlockSize; j++) {
                decryptBuf[j] ^= sBufLast[j];
            }
            System.arraycopy(sectorBuf, pos, sBufLast, 0, sBlockSize);
            System.arraycopy(decryptBuf, 0, sectorBuf, pos, sBlockSize);
            pos += sBlockSize;
        }
    }

    static public boolean encrypt(File obbInputFile, byte[] key, byte[] salt, File obbOutputFile) {
        Object fishKey;
        try {
            fishKey = Twofish_Algorithm.makeKey(key);
        } catch (InvalidKeyException e1) {
            e1.printStackTrace();
            return false;
        }
        if (!obbInputFile.exists()) {
            return false;
        }
        long fileLength = obbInputFile.length();
        // this means that it has an OBB footer. We have to parse it out.
        ObbFile obbFile = new ObbFile();
        obbFile.parseObbFile(obbInputFile);

        System.out.println("Filesystem length: " + obbFile.mFileSystemSize);
        fileLength = obbFile.mFileSystemSize;

        long numSectors = fileLength / BYTES_PER_SECTOR;
        byte[] sectorBytes = new byte[BYTES_PER_SECTOR];
        RandomAccessFile rafIn = null;
        RandomAccessFile rafOut = null;

        if (obbOutputFile.exists()) {
            System.out.println("Deleting Existing Output File");
            obbOutputFile.delete();
        }
        try {
            rafIn = new RandomAccessFile(obbInputFile, "r");
            rafOut = new RandomAccessFile(obbOutputFile, "rw");
            rafOut.seek(0);
            for (int sector = 0; sector < numSectors; sector++) {
                rafIn.seek(sector * EncryptedBlockFile.BYTES_PER_SECTOR);
                rafIn.readFully(sectorBytes);
                encryptSector(sector, sectorBytes, fishKey);
                rafOut.write(sectorBytes);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != rafIn) {
                try {
                    rafIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != rafOut) {
                try {
                    rafOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // salt
        obbFile.mSalt = salt;

        // set the salt flag
        obbFile.mFlags |= OBB_SALTED;
        obbFile.writeTo(obbOutputFile);
        return true;
    }

    static public boolean encrypt(File obbInputFile, byte[] key) {
        Object fishKey;
        try {
            fishKey = Twofish_Algorithm.makeKey(key);
        } catch (InvalidKeyException e1) {
            e1.printStackTrace();
            return false;
        }
        if (!obbInputFile.exists()) {
            return false;
        }
        long fileLength = obbInputFile.length();
        long numSectors = fileLength / BYTES_PER_SECTOR;
        byte[] sectorBytes = new byte[BYTES_PER_SECTOR];
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(obbInputFile, "rw");
            for (int sector = 0; sector < numSectors; sector++) {
                raf.seek(sector * EncryptedBlockFile.BYTES_PER_SECTOR);
                raf.readFully(sectorBytes);
                encryptSector(sector, sectorBytes, fishKey);
                raf.seek(sector * EncryptedBlockFile.BYTES_PER_SECTOR);
                raf.write(sectorBytes);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != raf) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    static public boolean decrypt(File obbInputFile, File outFile, byte[] key) {
        Object fishKey;
        try {
            fishKey = Twofish_Algorithm.makeKey(key);
        } catch (InvalidKeyException e1) {
            e1.printStackTrace();
            return false;
        }
        ObbFile obbFile = new ObbFile();
        obbFile.parseObbFile(obbInputFile);

        System.out.println("Filesystem length: " + obbFile.mFileSystemSize);

        long numSectors = obbFile.mFileSystemSize / BYTES_PER_SECTOR;
        byte[] sectorBytes = new byte[BYTES_PER_SECTOR];
        RandomAccessFile rafOut = null;
        RandomAccessFile rafIn = null;
        try {
            rafOut = new RandomAccessFile(outFile, "rw");
            rafIn = new RandomAccessFile(obbInputFile, "rw");
            for (int sector = 0; sector < numSectors; sector++) {
                rafIn.seek(sector * BYTES_PER_SECTOR);
                rafIn.readFully(sectorBytes);
                decryptSector(sector, sectorBytes, fishKey);
                rafOut.write(sectorBytes);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != rafOut) {
                try {
                    rafOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != rafIn) {
                try {
                    rafIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // clear salt
        byte[] salt = obbFile.mSalt;
        for (int i = 0; i < salt.length; i++) {
            salt[i] = 0;
        }
        // clear the salt flag
        obbFile.mFlags &= ~OBB_SALTED;
        obbFile.writeTo(outFile);
        return true;
    }

    public boolean writeTo(String fileName)
    {
        File obbFile = new File(fileName);
        return writeTo(obbFile);
    }

    public boolean writeTo(File obbFile) {
        if (!obbFile.exists())
            return false;

        try {

            long fileLength = obbFile.length();
            RandomAccessFile raf = new RandomAccessFile(obbFile, "rw");
            raf.seek(fileLength);

            if (null == mPackageName || mPackageVersion == -1) {
                throw new RuntimeException("tried to write uninitialized ObbFile data");
            }

            FileChannel fc = raf.getChannel();
            ByteBuffer bbInt = ByteBuffer.allocate(4);
            bbInt.order(ByteOrder.LITTLE_ENDIAN);
            bbInt.putInt(kSigVersion);
            bbInt.rewind();
            fc.write(bbInt);

            bbInt.rewind();
            bbInt.putInt((int) mPackageVersion);
            bbInt.rewind();
            fc.write(bbInt);

            bbInt.rewind();
            bbInt.putInt((int) mFlags);
            bbInt.rewind();
            fc.write(bbInt);

            raf.write(mSalt);

            bbInt.rewind();
            bbInt.putInt(mPackageName.length());
            bbInt.rewind();
            fc.write(bbInt);

            raf.write(mPackageName.getBytes());

            bbInt.rewind();
            bbInt.putInt(mPackageName.length() + kPackageNameOffset);
            bbInt.rewind();
            fc.write(bbInt);

            bbInt.rewind();
            bbInt.putInt((int) kSignature);
            bbInt.rewind();
            fc.write(bbInt);

            raf.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
