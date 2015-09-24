package com.martiansoftware.martifact.web;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author mlamb
 */
public class TableFormatter<T> {
    
    private enum Justification {LEFT, RIGHT};
    private final List<Renderer<T>> _renderers = new java.util.ArrayList<>();
    
    public TableFormatter<T> left(String columnName, Function<T, String> columnRenderer) {
        _renderers.add(new Renderer(columnName, columnRenderer, Justification.LEFT, _renderers.size()));
        return this;
    }

    public TableFormatter<T> right(String columnName, Function<T, String> columnRenderer) {
        _renderers.add(new Renderer(columnName, columnRenderer, Justification.RIGHT, _renderers.size()));
        return this;
    }

    public String format(Collection<T> items) {
        int[] widths = computeWidths(items);
        StringBuilder sb = new StringBuilder();
        
        sb.append(_renderers.stream().map(r ->  r.renderTitle(widths[r.index])).collect(Collectors.joining(" "))).append("\n");
        items.stream().forEach(i ->
            sb.append(_renderers.stream().map(r -> r.render(i, widths[r.index])).collect(Collectors.joining(" "))).append("\n")
        );
        return sb.toString();
    }
    
    private int[] computeWidths(Collection<T> items) {
        int[] w = new int[_renderers.size()];
        _renderers.stream().forEach(r -> w[r.index] = r.title.length());
        items.stream().forEach(item ->
            _renderers.stream().forEach(r -> w[r.index] = Math.max(w[r.index], r.renderFunction.apply(item).length()))
        );
        return w;
    }
    
    private class Renderer<T> {
        final String title;
        final Function<T, String> renderFunction;
        final Justification justification;
        final int index;
        
        Renderer(String title, Function<T, String> renderFunction, Justification justification, int index) {
            this.title = title == null ? "" : title;
            this.justification = justification;
            this.renderFunction = renderFunction;
            this.index = index;
        }
        
        String fmt(int width) { // create a format string with the right width and justification
            return String.format("%%%s%ds", justification == Justification.LEFT ? "-" : "", Math.max(width, 1));
        }
        
        String renderTitle(int width) {
            return String.format(fmt(width), title);
        }
        
        String render(T t, int width) {
            return String.format(fmt(width), renderFunction.apply(t));
        }
    }
}
