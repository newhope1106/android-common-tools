package cn.appleye.server.common;

import java.io.UnsupportedEncodingException;

import com.android.ddmlib.IShellOutputReceiver;

public abstract class SingleLineReceiver implements IShellOutputReceiver{

	private boolean mTrimLines = true;
	/** unfinished message line, stored for next packet */
    private String mUnfinishedLine = null;
    
    private boolean isCancel = false;
    
    /**
     * Set the trim lines flag.
     * @param trim whether the lines are trimmed, or not.
     */
    public void setTrimLine(boolean trim) {
        mTrimLines = trim;
    }
    
	@Override
	public void addOutput(byte[] data, int offset, int length) {
		// TODO Auto-generated method stub
		if (!isCancelled()) {
            String s = null;
            try {
                s = new String(data, offset, length, "UTF-8"); //$NON-NLS-1$
            } catch (UnsupportedEncodingException e) {
                // normal encoding didn't work, try the default one
                s = new String(data, offset,length);
            }
 
            // ok we've got a string
            // if we had an unfinished line we add it.
            if (mUnfinishedLine != null) {
                s = mUnfinishedLine + s;
                mUnfinishedLine = null;
            }
 
            // now we split the lines
            int start = 0;
            do {
                int index = s.indexOf("\r\n", start); //$NON-NLS-1$
 
                // if \r\n was not found, this is an unfinished line
                // and we store it to be processed for the next packet
                if (index == -1) {
                    mUnfinishedLine = s.substring(start);
                    break;
                }
 
                // so we found a \r\n;
                // extract the line
                String line = s.substring(start, index);
                if (mTrimLines) {
                    line = line.trim();
                }
                
                processLine(line);
                
                //System.out.println(line);
 
                // move start to after the \r\n we found
                start = index + 2;
            } while (true);
        }
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		done();
	}
	
	/**
	 * Terminates the process. This is called after the last lines have been through
     * {@link #processNewLines(String[])}.
     */
    public abstract void done();
    
    public void cancel(){
    	isCancel = true;
    }

	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return isCancel;
	}

	public abstract void processLine(String line); //按照单行，处理获取的信息
}
