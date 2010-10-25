/*
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *              http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nookdevs.crossword;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
//import java.util.StringTokenizer;
//import android.content.Context;
import android.util.Log;

public class PuzzlePuz {
    private String mChecksum;
    private String mFileMagic;
    private String mCIBCS;
    private String mMaskLowCS;
    private String mMaskHighCS;
    private String mVersion;
    private String mBIC;
    private int mRow;
    private int mCol;
    private int mNumClues;
    private String mSolString;
    private String mGrid;
    private String mTitle;
    private String mAuthor;
    private String mCopyRight;
    private ArrayList<Clue>  mClues;
    private String mNotes;
    private String mFile;
    private CrossWord mActivity;
    private ArrayList<int[]> mCircles;
    private ArrayList<int[]> mShades;
    private ArrayList<int[]> mRebusCells;
    private ArrayList<String> mRebusValues;
    private String mRebus;
    private String mRebusTable;
    private String mExtrasGrid;
    private boolean mHasRebus;
    private boolean mFormatSupportsRebus;
    private static final String LOGTAG=".Puz Parser:";
    
    public static final int VERSION_INDEX=0x18;
    public static final int COL_INDEX=0x2C;
    public static final int TITLE_INDEX=0x32;
//    public static void main(String []s ) {
//        PuzzlePuz p = new PuzzlePuz(null);
//        p.loadPuzzle("f:\\test.puz");
//    }
    public PuzzlePuz(CrossWord activity) {
        mActivity = activity;
    }
    Puzzle loadPuzzle(String file) {
        Puzzle puzzle = null ;
        try {
            if ( !file.toLowerCase().endsWith(".puz")) {
                Log.e(this.toString(), "Not a valid puz file: " + file);
                return null;
            }
            RandomAccessFile fp = new RandomAccessFile(file, "r");
            fp.seek(VERSION_INDEX);
            byte[] buffer = new byte[3];
            fp.read(buffer, 0, 3);
            mVersion = new String(buffer, "Windows-1252");
            if ( mVersion.equals("1.2") || mVersion.equals("1.1") || mVersion.equals("1.0") || mVersion.startsWith("0.") ) {
            	mFormatSupportsRebus = false ;
            } else {
            	//mFormatSupportsRebus = true ;
            	mFormatSupportsRebus = false ; // TODO: finish rebus support and change this
            }
            fp.seek(COL_INDEX);
            mCol = fp.readByte();
            //System.out.println("Rows =" + mRow);
            mRow = fp.readByte();
            //System.out.println("Cols =" + mCol);
            
            mNumClues = Short.reverseBytes(fp.readShort());
            //System.out.println("NumClues =" + mNumClues);
            fp.skipBytes(2);
            int scrambled = fp.readShort();
            if( scrambled != 0) {
                Log.e(this.toString(), "File appears to be scrambled...");
            }
            buffer = new byte[mRow * mCol];
            fp.read(buffer, 0,mRow*mCol);
            mSolString = new String(buffer, "Windows-1252");
            fp.read(buffer, 0, mRow*mCol);
            mGrid = new String(buffer,"Windows-1252");
            int size =(int)(fp.length() - fp.getFilePointer());
            buffer = new byte[size];
            fp.read(buffer, 0, size);
            String tmp = new String(buffer,"Windows-1252");
            String[] data= tmp.split("\0");
            if( data == null || data.length < 3+mNumClues) {
                Log.e(this.toString(), "File appears to be corrupted...");
                return null;
            }
            mTitle = data[0];
            mAuthor = data[1];
            mCopyRight = data[2];
            mClues = new ArrayList<Clue>(mNumClues);
            int i=3;
            int x=0;
            int y=0;
            int num=1;
            
            boolean accepted=false;
            while( true) {
                if( mGrid.charAt(x + mCol*y) == '.') {
                    // ignore
                } else {
                    if ( x ==0 || mGrid.charAt(x -1 + mCol*y) == '.') {
                        if( x < mCol-1 && mGrid.charAt(x+1 +mCol*y) !='.') {
                            Clue clue = new Clue(data[i++], CrossWord.ACROSS, num, y, x);
                            mClues.add(clue);
                            accepted=true;
                        }
                    }
                    if( y ==0 || mGrid.charAt( x + mCol*(y-1)) =='.') {
                        if( y < mRow-1 && mGrid.charAt(x +mCol*(y+1)) !='.') {
                            Clue clue = new Clue(data[i++], CrossWord.DOWN, num, y, x);
                            mClues.add(clue);
                            accepted=true;
                        }
                    }
                    if( accepted) {
                        num++;
                        accepted=false;
                    }
                }
                
                x++;
                if ( x == mCol) {
                    x=0;
                    y++;
                }
                if( y == mRow) {
                    break;
                }
                
            }
            if( i < data.length) {
                mNotes = data[i++];
            } else
                mNotes=null;
            int c=0;
            for(int k=0; k< i; k++) {
                c += data[k] != null?data[k].length()+1:1;
            }
            if( i < data.length){
                fp.close();
                loadExtraFields(buffer, c);
            } else {
                fp.close();
            }
            
            puzzle = new Puzzle(mActivity, file, mRow, mCol, mSolString, mCircles, mShades,
            		mFormatSupportsRebus, null, null, mClues, mTitle, mAuthor, mCopyRight,null,null,null, null) ;
        } catch(Exception ex) {
            Log.e(this.toString(), "Error parsing file " + file + " " + ex.getMessage(), ex);
            return null;
        }
        return puzzle;
    }
    private void loadExtraFields(byte[] buffer, int c) {
        try {
            int i = c;
            while( i < buffer.length) {
                String keyword = ""+ (char)buffer[i++] + (char)buffer[i++] + (char)buffer[i++] + (char)buffer[i++];
                ByteBuffer bb = ByteBuffer.allocate(2); 
                bb.order(ByteOrder.LITTLE_ENDIAN); 
                bb.put(buffer[i++]); 
                bb.put(buffer[i++]); 
                int size = bb.getShort(0); 
                if( keyword != null && keyword.equals("GRBS")) {
                    //load rebus table
                    // skip checksum value
                    i+=2;
                    int len= mRow*mCol;
                    int csum=0;
                    for( int k=0; k< len; k++) {
                        csum += buffer[i+k];
                    }
                    if( csum !=0) {
                        mRebus = new String( buffer, i, mRow*mCol, "Windows-1252");
                        i+=len;
                        mHasRebus = true;
                        i+=1; //Nul 
                        i+=4; //RTBL keyword
                        bb = ByteBuffer.allocate(2); 
                        bb.order(ByteOrder.LITTLE_ENDIAN); 
                        bb.put(buffer[i++]); 
                        bb.put(buffer[i++]);
                        len = bb.getShort(0);
                        i+=2; //skip checksum again
                        mRebusTable = new String(buffer,i,len, "Windows-1252");
                        Log.d( this.toString(), "DEBUG: mRebusTable = \"" + mRebusTable + "\"" ) ;  // TODO: remove this
                        i+=1;// for skipping NUL.
                    }
                } else if(keyword.equals("GEXT")) {
                    i+=2; //Checksum
                    int len = mRow*mCol;
                    mExtrasGrid = new String( buffer,i, len,"Windows-1252");
                    mCircles = new ArrayList<int[]>();
                    for(int k=0; k< len; k++) {
                        int c1 = buffer[k+i];
                        if( c1 !=0) {
                            int [] pos = new int[2];
                            pos[0] = (k)/mRow;
                            pos[1] = (k) - pos[0]*mRow;
                            mCircles.add(pos);
                        }
                    }
                    i+=len;
                    i++; //Null
                } else if(keyword.equals("RUSR")) {
                    i+=2;//checksum
                    i+=size;
                    i+=1; //Null
                } else if( keyword.equals("LTIM")) {
                    i+= 2; //checksum
                    i+= size;
                    i+=1; //Null
                }
                else {
                    Log.e(LOGTAG, "Unknown section in file. Ignoring... " + keyword);
                    i+=2; //assuming same structure as others.
                    i+=size;
                    i+=1;
                }
              
            }
        } catch(Exception ex) {
            Log.e(LOGTAG, "Exception while reading extra data:"+ex.getMessage(), ex);
        }
    }
}
