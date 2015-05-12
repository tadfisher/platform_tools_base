package com.android.tests.basic;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;

import java.io.InputStream;
import com.android.tests.basic.StringProvider;

public class Main extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TextView tv = (TextView) findViewById(R.id.dateText);

        String message;
        try {
            // load untouched xml file.
            InputStream inputStream = null;
            try {
                inputStream = getClass().getResourceAsStream("/com/android/tests/other/some.xml");
                if (inputStream == null) {
                    message = "Cannot load some.xml";
                } else {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(inputStream);
                    String value = doc.getDocumentElement().getAttribute("value");
                    message = StringProvider.getString(Integer.parseInt(value));
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }

            tv.setText(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
