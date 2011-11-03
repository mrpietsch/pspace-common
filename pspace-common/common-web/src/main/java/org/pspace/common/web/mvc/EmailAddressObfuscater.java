package org.pspace.common.web.mvc;


import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;


/**
 * @author peach
 */
public class EmailAddressObfuscater {

    private static interface StringConverter {
        String convert(String s);
    }

    private static class Pair {

        private Pair(StringConverter rb, String js) {
            this.rb = rb;
            this.js = js;
        }

        StringConverter rb;
        String js;

    }

    public static String enkode(String html) {

        int max_length = 1024;

        Pair[] kodes = {

                (new Pair(new StringConverter() {
                    @Override
                    public String convert(String s) {
                        return StringUtils.reverse(s);
                    }
                }, ";kode=kode.split('').reverse().join('')")),

                (new Pair(new StringConverter() {
                    @Override
                    public String convert(String s) {

                        StringBuilder result = new StringBuilder();

                        for (byte b : s.getBytes()) {
                            b += 3;
                            if (b > 127) b -= 128;
                            result.append((char) b);
                        }
                        return result.toString();

                    }
                }, ";x='';for(i=0;i<kode.length;i++){c=kode.charCodeAt(i)-3;if(c<0)c+=128;x+=String.fromCharCode(c)}kode=x")),

                (new Pair(new StringConverter() {
                    @Override
                    public String convert(String s) {

                        char[] chars = s.toCharArray();

                        for (int i = 0; i <= (s.length() / 2 - 1); i++) {
                            char h = chars[i * 2];
                            chars[i * 2] = chars[i * 2 + 1];
                            chars[i * 2 + 1] = h;
                        }

                        return new String(chars);

                    }
                }, ";x='';for(i=0;i<(kode.length-1);i+=2){x+=kode.charAt(i+1)+kode.charAt(i)}kode=x+(i<kode.length?kode.charAt(kode.length-1):'');"))
        };


        String kode = "document.write(" + js_dbl_quote(html) + ");";

        if (max_length <= kode.length()) {
            max_length = kode.length() + 1;
        }

        String result = "";


        // while (kode.length() < max_length) {
        int i = 0;
        // TODO more than one iteration produces wrong code
        while (i < 1) {
            i++;
            int idx = (int) (Math.random() * kodes.length);
            kode = String.format("kode=%s%s", js_dbl_quote(kodes[idx].rb.convert(kode)), kodes[idx].js);

            if (result.length() <= max_length) {
                StringBuilder js = new StringBuilder("<script type=\"text/javascript\">\n/* <![CDATA[ */\nfunction hivelogic_enkoder(){var kode=\n");
                js.append(js_wrap_quote(js_dbl_quote(kode), 79));
                js.append("\n;var i,c,x;while(eval(kode));}hivelogic_enkoder();\n/* ]]> */\n</script>");
                result = js.toString();
            }

        }

        return result;

    }

    private static String js_dbl_quote(String str) {
        return "\"" + StringEscapeUtils.escapeJavaScript(str) + "\"";
    }

    public static String enkode_mail(String email, String link_text) {
        return enkode_mail(email, link_text, null, null);

    }

    public static String enkode_mail(String email, String link_text, String title_text, String subject) {
        StringBuilder str = new StringBuilder();

        str.append("<a href=\"mailto:").append(email);
        if (subject != null && !subject.isEmpty()) str.append("?subject=").append(subject);

        str.append("\" title=\"");
        if (title_text != null && !title_text.isEmpty()) str.append(title_text);
        str.append("\">").append(link_text).append("</a>");
        return enkode(str.toString());
    }


    private static String js_wrap_quote(String str, int max_line_length) {

        max_line_length -= 3;

        int lineLen = 0;
        StringBuilder result = new StringBuilder();


        while (str.length() > 0) {
            final String chunk;

            if (str.startsWith("\\u")) {
                chunk = str.substring(0, 6);
                str = str.substring(6);
            } else if (str.startsWith("\\")) {
                chunk = str.substring(0, 2);
                str = str.substring(2);
            } else {
                chunk = str.substring(0, 1);
                str = str.substring(1);
            }

            if (lineLen + chunk.length() >= max_line_length) {
                result.append("\"+\n\"");
                lineLen = 1;
            }

            lineLen += chunk.length();
            result.append(chunk);
        }
        return result.toString();

    }

    public static void main(String[] args) {
        System.out.println(EmailAddressObfuscater.enkode_mail("martin@p-sp\u00fc\u00fc\u00fc\u00fc\u00fc\u00fcace.\u00fc\u00fc\u00fc\u00fcorg", "Martin Pietsch"));
    }


}
