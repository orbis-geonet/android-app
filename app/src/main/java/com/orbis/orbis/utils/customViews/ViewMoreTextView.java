package com.orbis.orbis.utils.customViews;

import android.content.Context;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.abdulhakeem.seemoretextview.R.color;
import com.orbis.orbis.R;

public class ViewMoreTextView extends AppCompatTextView {
    ClickableSpan clickableSpan;
    private Integer textMaxLength = 150;
    private Integer seeMoreTextColor;
    private String collapsedTextWithSeeMoreButton;
    private String expandedTextWithSeeMoreButton;
    private String orignalContent;
    private SpannableString collapsedTextSpannable;
    private SpannableString expandedTextSpannable;
    private Boolean isExpanded;
    private String seeMore;
    private String seeLess;

    public ViewMoreTextView(Context context) {
        super(context);
        this.seeMoreTextColor = color.seemore_color;
        this.isExpanded = false;
        this.seeMore = "view more";
        this.seeLess = "view less";
        this.clickableSpan = new NamelessClass_1();
    }

    public ViewMoreTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.seeMoreTextColor = color.seemore_color;
        this.isExpanded = false;
        this.seeMore = "view more";
        this.seeLess = "view less";
        this.clickableSpan = new NamelessClass_1();
    }

    public ViewMoreTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.seeMoreTextColor = color.seemore_color;
        this.isExpanded = false;
        this.seeMore = "view more";
        this.seeLess = "view less";

        class NamelessClass_1 extends ClickableSpan {
            NamelessClass_1() {
            }

            public void onClick(View widget) {
                ViewMoreTextView.this.toggle();
            }

            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(ContextCompat.getColor(context, R.color.view_more_blue));
            }
        }

        this.clickableSpan = new NamelessClass_1();
    }

    public void setTextMaxLength(Integer maxLength) {
        this.textMaxLength = maxLength;
    }

    public void setSeeMoreTextColor(Integer color) {
        this.seeMoreTextColor = color;
    }

    public void expandText(Boolean expand) {
        if (expand) {
            this.isExpanded = true;
            this.setText(this.expandedTextSpannable);
        } else {
            this.isExpanded = false;
            this.setText(this.collapsedTextSpannable);
        }

    }

    public void setSeeMoreText(String seeMoreText, String seeLessText) {
        this.seeMore = seeMoreText;
        this.seeLess = seeLessText;
    }

    public Boolean isExpanded() {
        return this.isExpanded;
    }

    public void toggle() {
        if (this.isExpanded) {
            this.isExpanded = false;
            this.setText(this.collapsedTextSpannable);
        } else {
            this.isExpanded = true;
            this.setText(this.expandedTextSpannable);
        }

    }

    public void setContent(String text) {
        try {
            this.orignalContent = text;
            this.setMovementMethod(LinkMovementMethod.getInstance());
            Log.d("textFound", text);
            if (this.orignalContent.length() >= this.textMaxLength) {
                this.collapsedTextWithSeeMoreButton = this.orignalContent.substring(0, this.textMaxLength) + "... " + this.seeMore;
                this.expandedTextWithSeeMoreButton = this.orignalContent + " " + this.seeLess;
                this.collapsedTextSpannable = new SpannableString(this.collapsedTextWithSeeMoreButton);
                this.expandedTextSpannable = new SpannableString(this.expandedTextWithSeeMoreButton);
                this.collapsedTextSpannable.setSpan(this.clickableSpan, this.textMaxLength + 4, this.collapsedTextWithSeeMoreButton.length(), 0);
                this.collapsedTextSpannable.setSpan(new StyleSpan(2), this.textMaxLength + 4, this.collapsedTextWithSeeMoreButton.length(), 0);
                this.collapsedTextSpannable.setSpan(new RelativeSizeSpan(0.9F), this.textMaxLength + 4, this.collapsedTextWithSeeMoreButton.length(), 0);
                this.expandedTextSpannable.setSpan(this.clickableSpan, this.orignalContent.length() + 1, this.expandedTextWithSeeMoreButton.length(), 0);
                this.expandedTextSpannable.setSpan(new StyleSpan(2), this.orignalContent.length() + 1, this.expandedTextWithSeeMoreButton.length(), 0);
                this.expandedTextSpannable.setSpan(new RelativeSizeSpan(0.9F), this.orignalContent.length() + 1, this.expandedTextWithSeeMoreButton.length(), 0);
                if (this.isExpanded) {
                    this.setText(this.expandedTextSpannable);
                } else {
                    this.setText(this.collapsedTextSpannable);
                }
            } else {
                this.setText(this.orignalContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class NamelessClass_1 extends ClickableSpan {
        NamelessClass_1() {
        }

        public void onClick(View widget) {
            ViewMoreTextView.this.toggle();
        }

        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setColor(getResources().getColor(R.color.view_more_blue));
        }
    }
}

