package org.pspace.common.web.mvc;

import org.springframework.context.i18n.LocaleContextHolder;

import java.beans.PropertyEditorSupport;
import java.util.HashSet;
import java.util.Locale;

/**
 * @author Martin Pietsch
 */
public class PhoneEditor extends PropertyEditorSupport {

    private Locale locale = LocaleContextHolder.getLocale();

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    private final static String[][] CODES_TABLE = {
            // CC,AREA,AREAL,SUBSC,SUBSL,EXTL
            {"49", "30", "2", null, null, null},
            {"49", "30", "2", "450", "3", "6"},
            {"49", "30", "2", "450", "3", "7"},
            {"49", "171", "3", null, null, null},
            {"49", "172", "3", null, null, null},
            {"49", "173", "3", null, null, null},
            {"49", "174", "3", null, null, null},
            {"49", "175", "3", null, null, null},
            {"49", "176", "3", null, null, null},
            {"49", "177", "3", null, null, null},
            {"49", "178", "3", null, null, null},
            {"49", "179", "3", null, null, null},
            {"49", null, "2", null, null, null},
            {"49", null, "3", null, null, null},
            {"49", null, "4", null, null, null},
            {"49", null, "5", null, null, null},
            {"1", null, "3", null, "7", null},
            {"1", "412", "3", null, "7", null},
            {"1", "412", "3", "383", "3", "4"},
            {"242", null, null, null, null, null},
    };

    private static String validate(String phoneNumber) {

        String countryCode;
        String areaCode = null;
        String subscriber = null;

        // processing the country code
        if (phoneNumber.startsWith("+")) {

            if (existsCountry(phoneNumber.substring(1, 4))) {
                countryCode = phoneNumber.substring(1, 4);
                phoneNumber = phoneNumber.substring(4, phoneNumber.length());
            } else if (existsCountry(phoneNumber.substring(1, 3))) {
                countryCode = phoneNumber.substring(1, 3);
                phoneNumber = phoneNumber.substring(3, phoneNumber.length());
            } else if (existsCountry(phoneNumber.substring(1, 2))) {
                countryCode = phoneNumber.substring(1, 2);
                phoneNumber = phoneNumber.substring(2, phoneNumber.length());
            } else {
                throw new IllegalArgumentException("Unknown country code");
            }
        } else {
            // could not parse that number
            return phoneNumber;
        }

        // processing the area code
        HashSet<String> avAC = new HashSet<String>();
        HashSet<Byte> avACL = new HashSet<Byte>();

        for (String[] entry : CODES_TABLE) {
            if (entry[0].equals(countryCode)) {

                String area = entry[1];

                if (area != null) {
                    avAC.add(area);
                } else {
                    // no explicit area code... considering this as a rule...
                    try {
                        Byte areaLengthRule = Byte.parseByte(entry[2]);
                        avACL.add(areaLengthRule);
                    } catch (NumberFormatException e2) {
                        // no explicit rule... every length is allowed...
                        avACL.add((byte) 0);
                    }
                }

            }
        }

        final boolean areaLengthFree = avACL.remove((byte) 0);
        boolean areaCodeFound = false;


        for (String availableAreaCode : avAC) {
            if (phoneNumber.startsWith(availableAreaCode)) {
                // found
                int areaLength = availableAreaCode.length();
                areaCodeFound = true;
                areaCode = phoneNumber.substring(0, areaLength);
                phoneNumber = phoneNumber.substring(areaLength, phoneNumber.length());
            }
        }

        if (areaCode == null) {
            if (areaLengthFree)
                return "+" + countryCode + " " + phoneNumber; // cannot parse further
            else if (avACL.size() > 0) {
                // there are rules for the length of the area code

                byte minSize = Byte.MAX_VALUE;
                for (byte size : avACL) if (size < minSize) minSize = size;

                if (phoneNumber.length() < minSize) {
                    throw new IllegalArgumentException("Area code not extractable because number part is too short: " + phoneNumber);
                }

                // is there a distinct rule for the length of the area code?
                if (avACL.size() == 1) {
                    byte areaLength = (Byte) avACL.toArray()[0];
                    areaCode = phoneNumber.substring(0, areaLength);
                    phoneNumber = phoneNumber.substring(areaLength, phoneNumber.length());
                } else {
                    return "+" + countryCode + " " + phoneNumber; // cannot parse further
                }

            } else
                throw new IllegalArgumentException("Invalid area code");
        }

        // *** SUBSCRIBER PART *** //

        // area code is not null

        HashSet<String> avS = new HashSet<String>();
        HashSet<Byte> avSL = new HashSet<Byte>();

        for (String[] entry : CODES_TABLE) {

            final boolean countryCodeConstraint = entry[0].equals(countryCode);
            final boolean areaCodeConstraint = (areaCodeFound && areaCode.equals(entry[1])) || (!areaCodeFound && entry[1] == null);

            if (countryCodeConstraint && areaCodeConstraint) {

                // possible rules for subscribers
                String subs = entry[3];

                if (subs != null) {
                    avS.add(subs);

                } else {
                    // no explicit subscriber... considering this as a rule...
                    try {
                        Byte areaLengthRule = Byte.parseByte(entry[4]);
                        avSL.add(areaLengthRule);
                    } catch (NumberFormatException e2) {
                        // no explicit rule... every length is allowed...
                        avSL.add((byte) 0);
                    }
                }
            }

        }

        final boolean subscriberLengthFree = avSL.remove((byte) 0);
        boolean subscriberFound = false;

        for (String availableSubscriber : avS) {
            if (phoneNumber.startsWith(availableSubscriber)) {
                int subscriberLength = availableSubscriber.length();
                subscriberFound = true;
                subscriber = phoneNumber.substring(0, subscriberLength);
                phoneNumber = phoneNumber.substring(subscriberLength, phoneNumber.length());
            }
        }

        if (subscriber == null) {
            if (subscriberLengthFree)
                return "+" + countryCode + " " + areaCode + " " + phoneNumber;
            else if (avSL.size() > 0) {
                // there are rules for the length of the subscriber

                byte minSize = Byte.MAX_VALUE;
                for (byte size : avSL) if (size < minSize) minSize = size;

                if (phoneNumber.length() < minSize) {
                    throw new IllegalArgumentException("Subscriber part too short: " + phoneNumber);
                }

                // is there a distinct rule for the length of the area code?
                if (avSL.size() == 1) {
                    byte subscriberLength = (Byte) avSL.toArray()[0];
                    subscriber = phoneNumber.substring(0, subscriberLength);
                    phoneNumber = phoneNumber.substring(subscriberLength, phoneNumber.length());
                } else {
                    return "+" + countryCode + " " + areaCode + " " + phoneNumber;
                }
            } else {
                throw new IllegalArgumentException("Subscriber part has invalid length: " + phoneNumber + " Valid length are: " + avSL);
            }
        }

        // *** EXTENSION LENGTH RULE *** //


        HashSet<Byte> avExtL = new HashSet<Byte>();

        for (String[] entry : CODES_TABLE) {

            final boolean countryCodeConstraint = entry[0].equals(countryCode);
            final boolean areaCodeConstraint = (areaCodeFound && areaCode.equals(entry[1])) || (!areaCodeFound && entry[1] == null);
            final boolean subscriberConstraint = (subscriberFound && subscriber.equals(entry[3])) || (!subscriberFound && entry[3] == null);

            if (countryCodeConstraint && areaCodeConstraint && subscriberConstraint) {
                // no explicit subscriber... considering this as a rule...
                try {
                    Byte extensionLengthRule = Byte.parseByte(entry[5]);
                    avExtL.add(extensionLengthRule);
                } catch (NumberFormatException e) {
                    // no explicit rule... every length is allowed...
                    avExtL.add((byte) 0);
                }
            }
        }


        final boolean extensionLengthFree = avExtL.remove((byte) 0);


        if (extensionLengthFree || avExtL.contains((byte) phoneNumber.length())) {
            return "+" + countryCode + " " + areaCode + " " + subscriber + (!"".equals(phoneNumber) ? ("-" + phoneNumber) : "");
        }

        throw new IllegalArgumentException("Invalid phone number extension: " + phoneNumber);


    }

    private static String sanitize(String phoneNumber) {
        char[] characters = phoneNumber.toCharArray();

        StringBuilder phoneBuilder = new StringBuilder();

        // process every single character
        for (int i = 0; i < characters.length; i++) {

            char currentChar = characters[i];

            if (currentChar >= '0' && currentChar <= '9') {
                phoneBuilder.append(currentChar);
            } else if (i == 0 && currentChar == '+') {
                phoneBuilder.append(currentChar);
            } else {

                switch (currentChar) {
                    case 'a':
                    case 'A':
                    case 'b':
                    case 'B':
                    case 'c':
                    case 'C':
                        phoneBuilder.append('2');
                        break;
                    case 'd':
                    case 'D':
                    case 'e':
                    case 'E':
                    case 'f':
                    case 'F':
                        phoneBuilder.append('3');
                        break;
                    case 'g':
                    case 'G':
                    case 'h':
                    case 'H':
                    case 'i':
                    case 'I':
                        phoneBuilder.append('4');
                        break;
                    case 'j':
                    case 'J':
                    case 'k':
                    case 'K':
                    case 'l':
                    case 'L':
                        phoneBuilder.append('5');
                        break;
                    case 'm':
                    case 'M':
                    case 'n':
                    case 'N':
                    case 'o':
                    case 'O':
                        phoneBuilder.append('6');
                        break;
                    case 'p':
                    case 'P':
                    case 'q':
                    case 'Q':
                    case 'r':
                    case 'R':
                    case 's':
                    case 'S':
                        phoneBuilder.append('7');
                        break;
                    case 't':
                    case 'T':
                    case 'u':
                    case 'U':
                    case 'v':
                    case 'V':
                        phoneBuilder.append('8');
                        break;
                    case 'w':
                    case 'W':
                    case 'x':
                    case 'X':
                    case 'y':
                    case 'Y':
                    case 'z':
                    case 'Z':
                        phoneBuilder.append('9');
                        break;
                    case '(':
                    case ')':
                    case '-':
                    case ' ':
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal character '" + currentChar + "' in phone number.");
                }

            }

        }

        return phoneBuilder.toString();
    }

    private static String findIddForLocale(Locale locale) {
        return locale.equals(Locale.GERMANY) ? "00" : "011";
    }

    private static String replaceIDDByPlus(String phoneNumber, Locale locale) {

        // there is not IDD if there is a +
        if (phoneNumber.startsWith("+")) {
            return phoneNumber;
        }

        final String idd = findIddForLocale(locale);

        return phoneNumber.startsWith(idd) ? "+" + phoneNumber.substring(idd.length()) : phoneNumber;
    }

    private static boolean existsCountry(String s) {
        for (String[] entry : CODES_TABLE) {
            if (s.equals(entry[0])) return true;
        }
        return false;
    }

    /**
     * Sets the property value by parsing a given String.  May raise
     * java.lang.IllegalArgumentException if either the String is
     * badly formatted or if this kind of property can't be expressed
     * as text.
     *
     * @param text The string to be parsed.
     */
    @Override
    public void setAsText(String text) throws IllegalArgumentException {

        String phoneNumer = validate(replaceIDDByPlus(sanitize(text), locale));
        setValue(phoneNumer);

    }
}
