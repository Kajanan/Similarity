
package com.assign.chunk;

final class Tags {

    public static final String OUT_TAG = "O";

    public static final String START_TAG = OUT_TAG;

  
    public static final String START_TOKEN = ".";

    private static final String START_PREFIX = "B-";

    private static final String IN_PREFIX = "I-";

    private static final int PREFIX_LENGTH = 2;

    private Tags() { 
        
    }

    public static String baseTag(String tag) {
        return (tag.startsWith(START_PREFIX) || tag.startsWith(IN_PREFIX))
            ? stripPrefix(tag)
            : tag;
    }

 
    public static boolean equalBaseTags(String tag1, String tag2) {
        return baseTag(tag1).equals(baseTag(tag2));
    }

 
    public static boolean isStartTag(String tag) {
        return tag.startsWith(START_PREFIX);
    }

    public static boolean isInnerTag(String tag) {
        return tag.startsWith(IN_PREFIX);
    }

    public static boolean isOutTag(String tag) {
        return tag.equals(OUT_TAG);
    }

    public static boolean isMidTag(String tag1, String tag2) {
        return Tags.isInnerTag(tag2);
    }

    public static boolean illegalSequence(String tag1, String tag2) {
        return ( Tags.isInnerTag(tag2)
                 && !Tags.equalBaseTags(tag1,tag2) );
    }

    public static String toStartTag(String tag) {
        if (isOutTag(tag) || isStartTag(tag)) return tag;
        return START_PREFIX + tag;
    }


    public static String toInnerTag(String tag) {
    return isOutTag(tag) 
        ? tag 
        : IN_PREFIX + baseTag(tag);
    }

    private static String stripPrefix(String tag) {
        return tag.substring(PREFIX_LENGTH);
    }

    public static String PERSON_TAG = "PERSON";

  
    public static String LOCATION_TAG = "LOCATION";

    public static String ORGANIZATION_TAG = "ORGANIZATION";

    public static String OTHER_TAG = "OTHER";

    public static String MALE_PRONOUN_TAG = "MALE_PRONOUN";

    public static String FEMALE_PRONOUN_TAG = "FEMALE_PRONOUN";

    public static String DATABASE_MATCH_TAG_XDC = "USER_ENTITY_XDC1";

    public static String DATABASE_MATCH_TAG_NO_XDC = "USER_ENTITY_XDC0";

    public static final String[] TAG_SET = new String[] {
        PERSON_TAG,
        LOCATION_TAG,
        ORGANIZATION_TAG,
        OTHER_TAG,
        MALE_PRONOUN_TAG,
        FEMALE_PRONOUN_TAG,
        DATABASE_MATCH_TAG_XDC,
        DATABASE_MATCH_TAG_NO_XDC
    };

}
