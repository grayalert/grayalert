package com.github.grayalert.core;


import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

@Service
public class MessageTokeniser {

    public static final String DELIM = " ,:{}\n\t;=";

    public List<String> progressiveSplit(String input) {
        StringTokenizer tokenizer = new StringTokenizer(input, DELIM, true);
        List<String> result = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            current.append(tokenizer.nextToken());
            if (tokenizer.hasMoreElements()) {
                current.append(tokenizer.nextToken());
            }
            result.add(current.toString());
        }

        return result;
    }
}
