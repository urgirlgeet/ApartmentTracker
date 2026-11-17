package com.apartment.tracker.profile;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ApartmentAddress {
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^([A-Z])-([1-8])$");
    private static final List<String> BLOCKS = java.util.stream.IntStream.rangeClosed('A', 'Z')
        .mapToObj(value -> String.valueOf((char) value))
        .toList();
    private static final List<Integer> APARTMENT_NUMBERS = List.of(1, 2, 3, 4, 5, 6, 7, 8);

    private ApartmentAddress() {
    }

    public static List<String> blocks() {
        return BLOCKS;
    }

    public static List<Integer> apartmentNumbers() {
        return APARTMENT_NUMBERS;
    }

    public static String format(String block, Integer apartmentNumber) {
        return block.toUpperCase() + "-" + apartmentNumber;
    }

    public static boolean isValidSelection(String block, Integer apartmentNumber) {
        return block != null && block.matches("^[A-Z]$")
            && apartmentNumber != null && apartmentNumber >= 1 && apartmentNumber <= 8;
    }

    public static String[] parse(String address) {
        if (address == null) {
            return new String[] {"", ""};
        }
        Matcher matcher = ADDRESS_PATTERN.matcher(address.trim().toUpperCase());
        if (!matcher.matches()) {
            return new String[] {"", ""};
        }
        return new String[] {matcher.group(1), matcher.group(2)};
    }

    public static List<String> allApartments() {
        return BLOCKS.stream()
            .flatMap(block -> APARTMENT_NUMBERS.stream().map(number -> format(block, number)))
            .toList();
    }

    public static boolean isValid(String address) {
        return address != null && ADDRESS_PATTERN.matcher(address.trim().toUpperCase()).matches();
    }
}
