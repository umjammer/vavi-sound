/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;


/**
 * SeekAndPhraseInfo Chunk.
 * <pre>
 * "[MA]spI"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano initial version <br>
 */
public class SeekAndPhraseInfoChunk extends Chunk {

    private static final Logger logger = getLogger(SeekAndPhraseInfoChunk.class.getName());

    /** */
    public SeekAndPhraseInfoChunk(byte[] id, int size) {
        super(id, size);
//logger.log(Level.TRACE, "SeekAndPhraseInfo: " + size);
    }

    /** */
    public SeekAndPhraseInfoChunk() {
        System.arraycopy("spI".getBytes(), 0, id, 1, 3);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        byte[] data = new byte[size];
logger.log(Level.DEBUG, "SeekAndPhraseInfo: " + size + " bytes (subData)");
        dis.readFully(data);

        int i = 0;
        while (i < size) {
//logger.log(Level.TRACE, i + " / " + option.length + "\n" + StringUtil.getDump(option, i, option.length - i));
            SubData subDatum = new SubData(data, i);
            subData.put(subDatum.getTag(), subDatum);
logger.log(Level.DEBUG, "SeekAndPhraseInfo: subData: " + subDatum);
            i += 2 + 1 + subDatum.getData().length + 1; // tag ':' data ','
//logger.log(Level.TRACE, i + " / " + option.length + "\n" + StringUtil.getDump(option, i, option.length - i));
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        for (SubData subDatum : subData.values()) {
            subDatum.writeTo(os);
        }
    }

    /** */
    private final Map<String, SubData> subData = new TreeMap<>();

    /** */
    private static final String TAG_StartPoint = "st";

    /** */
    public int getStartPoint() {
        return ByteBuffer.wrap(subData.get(TAG_StartPoint).getData()).getInt();
    }

    /** */
    public void setStartPoint(int startPoint) {
        addSubData(TAG_StartPoint, new SubData(TAG_StartPoint, ByteBuffer.allocate(4).putInt(startPoint).array()));
    }

    /** */
    private static final String TAG_StopPoint = "sp";

    /** */
    public int getStopPoint() {
        return ByteBuffer.wrap(subData.get(TAG_StopPoint).getData()).getInt();
    }

    /** */
    public void setStopPoint(int stopPoint) {
        addSubData(TAG_StopPoint, new SubData(TAG_StopPoint, ByteBuffer.allocate(4).putInt(stopPoint).array()));
    }

    /** */
    private void addSubData(String tag, SubData subDatum) {
        subData.put(tag, subDatum);
        size += subDatum.getSize();
    }

//    /** */
//    private static final String[] TAG_Phrase = {
//        "Pa", "Pb", "Pc", "Pd", "Pe", "Pf", "Pg",
//        "Ph", "Pi", "Pj", "Pk", "Pl", "Pm", "Pn",
//        "Po", "Pp", "Pq", "Pr", "Ps", "Pt", "Pu",
//        "Pv", "Pw", "Px", "Py", "Pz",
//        "PA", "PB", "PE", "PI", "PK", "PS", "PR"
//    };

    /** TODO List */
    public List<SubData> getPhraseList() {
        return null;
    }

//    /** [a-zABEIKSR]+ */
//    private static final String TAG_SubSequence = "SL";

    /** TODO List */
    public List<SubData> getSubSequenceList() {
        return null;
    }
}
