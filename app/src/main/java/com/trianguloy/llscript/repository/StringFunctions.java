package com.trianguloy.llscript.repository;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by TrianguloY on 26/01/2015.
 * Reads a given Resource to a string
 */
public class StringFunctions {
    //From http://stackoverflow.com/questions/4087674/android-read-text-raw-resource-file
    public static String getRawFile(Context ctx, int resId) {
        InputStream inputStream = ctx.getResources().openRawResource(resId);
        try{
        byte[] buff = new byte[1024];
        int read;
        StringBuilder text = new StringBuilder();
        while ((read = inputStream.read(buff)) > 0) {
            text.append(new String(buff, 0, read));
        }
        return text.toString();
        } catch (IOException e) {
            return null;
        }
        finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }




    public static class valueAndIndex {
        String value;
        int from;
        int to;
        valueAndIndex(String v,int f,int t){
            value=v;
            from=f;
            to=t;
        }
        valueAndIndex(){
            value=null;
            from=-1;
            to=-1;
        }
    }

    //This function returns the string between beggining and ending in source starting from index, and the position o the matches (including the searched strings). If backwards is true it uses lastIndexOf
    public static valueAndIndex findBetween(String source, String beggining, String ending, int index, boolean backwards){
        int start;
        int end;
        valueAndIndex notFound = new valueAndIndex();


        if(!backwards){

            start = source.indexOf(beggining,index==-1?0:index);
            if(start==-1) return notFound;
            start+=beggining.length();

            end = source.indexOf(ending,start);
            if(end==-1) return notFound;

        }else{

            end = source.lastIndexOf(ending,index==-1?source.length():index);
            if(end==-1) return notFound;

            start = source.lastIndexOf(beggining,end-beggining.length());
            if(start==-1) return notFound;
            start+=beggining.length();

        }

        return new valueAndIndex(source.substring(start,end),start-beggining.length(),end+ending.length());
    }

}
