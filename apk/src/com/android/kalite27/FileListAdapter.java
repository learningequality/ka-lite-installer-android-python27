package com.android.kalite27;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileListAdapter extends BaseAdapter {

    private Context mContext;
    private List<File> fileList;
    private LayoutInflater inflater;

    public FileListAdapter(Context context, ArrayList<File> fileArray) {
        this.mContext = context;
        this.fileList = fileArray;
        this.inflater = LayoutInflater.from(mContext);
    }
    
    @Override
    public int getCount() {
        return fileList.size();
    }

    @Override
    public Object getItem(int i) {
        return fileList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null) {
            view = inflater.inflate(R.layout.list_item, null);
        }
        TextView fileTitle = (TextView) view.findViewById(R.id.file_item_file_name);
        ImageView fileImage = (ImageView) view.findViewById(R.id.file_item_image_view);
        fileTitle.setText(fileList.get(i).getName());
        String fileExt = fileExt(fileList.get(i).toString());
        if(fileList.get(i).isDirectory()) {
            fileImage.setBackgroundDrawable(mContext.getResources()
                .getDrawable(R.drawable.ic_folder));
        } else {
            if (fileExt != null) {
                if (fileExt.equalsIgnoreCase(".doc")) {
                    fileImage.setBackgroundDrawable(mContext.getResources()
                            .getDrawable(R.drawable.ic_doc_file));
                } else if (fileExt.equalsIgnoreCase(".docx")) {
                    fileImage.setBackgroundDrawable(mContext.getResources()
                            .getDrawable(R.drawable.ic_docx_file));
                } else if (fileExt.equalsIgnoreCase(".xls")) {
                    fileImage.setBackgroundDrawable(mContext.getResources()
                            .getDrawable(R.drawable.ic_xls_file));
                } else if (fileExt.equalsIgnoreCase(".xlsx")) {
                    fileImage.setBackgroundDrawable(mContext.getResources()
                            .getDrawable(R.drawable.ic_xlsx_file));
                } else if (fileExt.equalsIgnoreCase(".xml")) {
                    fileImage.setBackgroundDrawable(mContext.getResources()
                            .getDrawable(R.drawable.ic_xml_file));
                } else if (fileExt.equalsIgnoreCase(".html")) {
                    fileImage.setBackgroundDrawable(mContext.getResources()
                            .getDrawable(R.drawable.ic_html_file));
                } else if (fileExt.equalsIgnoreCase(".pdf")) {
                    fileImage.setBackgroundDrawable(mContext.getResources()
                            .getDrawable(R.drawable.ic_pdf_file));
                } else if (fileExt.equalsIgnoreCase(".txt")) {
                    fileImage.setBackgroundDrawable(mContext.getResources()
                            .getDrawable(R.drawable.ic_txt_file));
                } else {
                    fileImage.setBackgroundDrawable(mContext.getResources()
                            .getDrawable(R.drawable.ic_default_file));
                }
            }
        }
        return view;
    }

    /**
     * Returns the file extension of a file.
     * @param url the file path
     * @return
     */
    private String fileExt(String url) {
        if (url.indexOf("?")>-1) {
            url = url.substring(0,url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") );
            if (ext.indexOf("%")>-1) {
                ext = ext.substring(0,ext.indexOf("%"));
            }
            if (ext.indexOf("/")>-1) {
                ext = ext.substring(0,ext.indexOf("/"));
            }
            return ext.toLowerCase();
        }
    }
}
