package me.albertonicoletti.latex;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.albertonicoletti.latex.activities.SettingsActivity;

/**
 * The editor, an EditText with extended functionality:
 * - Line count
 * - Syntax highlighting
 *
 * @author Alberto Nicoletti    albyx.n@gmail.com    https://github.com/albyxyz
 */
public class LatexEditor extends EditText {

    public final Pattern[] patterns = {
            Pattern.compile("([\\\\])(\\w+|['`\\\\]\\w*)(\\*)*", Pattern.MULTILINE),
            Pattern.compile("([{]).+([}])", Pattern.MULTILINE),
            Pattern.compile("([\\[]).+([\\]])", Pattern.MULTILINE),
            Pattern.compile("(%).*$", Pattern.MULTILINE),
            Pattern.compile("\\$([^\\$]*)\\$", Pattern.MULTILINE),
            Pattern.compile("\\$", Pattern.MULTILINE)
    };

    public final int[] patternColors = {
            getResources().getColor(R.color.latex_class),
            getResources().getColor(R.color.latex_keyword),
            getResources().getColor(R.color.latex_third),
            getResources().getColor(R.color.text_grey),
            getResources().getColor(R.color.green),
            getResources().getColor(R.color.light_green)
    };

    /** Painter used to draw numbers */
    private static final TextPaint numberPainter = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    /** Painter used to draw the line counter's background */
    private static final Paint backgroundPainter = new Paint();

    /** Line height */
    private float lineHeight;
    /** Line counter's padding top (it starts a little before the actual lines) */
    private float lineCountPaddingTop;
    /** Line counter's column width */
    private float lineCounterColumnWidth;
    /** Line counter's column right margin (the margin before the text starts) */
    private float lineCounterColumnMargin;

    public LatexEditor(Context context) {
        super(context);
        init();
    }

    public LatexEditor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public LatexEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        refreshFontSize();
    }

    public void refreshFontSize(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        final String fontSizeString = sharedPref.getString(SettingsActivity.FONT_SIZE, "");
        float scaledDensity = getContext().getResources().getDisplayMetrics().scaledDensity;
        float fontSize = Float.valueOf(fontSizeString);
        numberPainter.setTextSize((fontSize - 3) * scaledDensity);
        setTextSize(fontSize);
        lineHeight = getLineHeight();
        // Number's color
        numberPainter.setColor(getResources().getColor(R.color.text_grey));
        // Given a point, the numbers are drawn starting from the right
        numberPainter.setTextAlign(Paint.Align.RIGHT);
        lineCountPaddingTop = - (lineHeight * 0.10f);
        float paddingLeft = (lineHeight * 3f);
        float marginBeforeText = paddingLeft * 0.2f;
        // The column width is given by the total padding left less the margin before text
        lineCounterColumnWidth =  paddingLeft - marginBeforeText;
        lineCounterColumnMargin = lineCounterColumnWidth/6;
        setPadding((int) paddingLeft, 0, 0, 0);
    }

    /**
     * Highlights the text using Latex syntax
     * @param start Start index
     * @param end End index
     */
    public void highlightText(int start, int end){
        Editable editable = getText();
        clearSpans(editable);

        CharSequence s = getText().subSequence(start, end);

        for(int i = 0; i < patterns.length; i++){
            Matcher matcher = patterns[i].matcher(s);
            while(matcher.find()){
                if(matcher.group().length() > 0){
                    editable.setSpan(new ForegroundColorSpan(patternColors[i%patternColors.length]),
                            start + matcher.start(), start + matcher.end(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        backgroundPainter.setColor(getResources().getColor(R.color.editor_column));
        // Draws a colored background to the line counter column
        canvas.drawRect(0, 0, lineCounterColumnWidth, this.getBottom(), backgroundPainter);

        backgroundPainter.setColor(getResources().getColor(R.color.text_darkgrey));
        // Draws a right-border to the line counter column
        canvas.drawLine(lineCounterColumnWidth, 0, lineCounterColumnWidth, getBottom(), backgroundPainter);

        // The number that will be drawn
        int lineToDraw = 1;
        // Set to true if a line hasn't got a newline character
        boolean previousLineNoNewline = false;
        // Will draw a number aside each line in the EditText
        // The number won't be drawn if the previous line hasn't got a newline character
        for (int i = 0; i < getLineCount(); i++) {
            int currentLineStart = getLayout().getLineStart(i);
            int currentLineEnd = getLayout().getLineEnd(i);
            String currentLine = getText().subSequence(currentLineStart, currentLineEnd).toString();
            boolean containsNewLine = currentLine.contains("\n");
            // If the previous line contains a newline character the number it will draw the number
            if (!previousLineNoNewline) {
                canvas.drawText(String.valueOf(lineToDraw),
                        lineCounterColumnWidth - lineCounterColumnMargin,
                        ((i+1) * lineHeight) + lineCountPaddingTop,
                        numberPainter);
                if (!containsNewLine) {
                    previousLineNoNewline = true;
                }
                lineToDraw++;
                // When it finds a line containing a newline character, the next line will be drawn
            } else {
                if (containsNewLine) {
                    previousLineNoNewline = false;
                }
            }
        }
        super.onDraw(canvas);
    }

    /**
     * Routine to remove the colored spans.
     * @param e Editable string
     */
    private void clearSpans( Editable e ){
        // remove foreground color spans
        ForegroundColorSpan spans[] = e.getSpans(
                0,
                e.length(),
                ForegroundColorSpan.class);

        for( int n = spans.length; n-- > 0; )
            e.removeSpan( spans[n] );

        // remove background color spans
        /*
        BackgroundColorSpan spans[] = e.getSpans(
                0,
                e.length(),
                BackgroundColorSpan.class );
        for( int n = spans.length; n-- > 0; )
            e.removeSpan( spans[n] );
        */
    }

    /**
     * Returns the String representation of the text
     * @return the String representation of the text
     */
    public String getTextString(){
        return this.getText().toString();
    }

}