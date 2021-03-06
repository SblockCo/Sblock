package com.easterlyn.util.text;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

public abstract class StaticQuoteConsumer implements QuoteConsumer {

  private final Collection<Pattern> patterns;

  public StaticQuoteConsumer(Pattern pattern) {
    this.patterns = Collections.singleton(pattern);
  }

  public StaticQuoteConsumer(Pattern... patterns) {
    if (patterns.length == 0) {
      throw new UnsupportedOperationException("Include a pattern in the constructor, you numpty.");
    }
    this.patterns = Arrays.asList(patterns);
  }

  public StaticQuoteConsumer(Collection<Pattern> patterns) {
    this.patterns = patterns;
  }

  @Override
  public Iterable<Pattern> getPatterns() {
    return patterns;
  }
}
