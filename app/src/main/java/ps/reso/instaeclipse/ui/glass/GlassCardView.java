package ps.reso.instaeclipse.ui.glass;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.ShapeAppearanceModel;

import ps.reso.instaeclipse.R;

/**
 * A MaterialCardView that reads as a pane of glass: translucent fill (from Widget.IE.Card), a
 * hairline outline, a specular highlight that catches the top rim and an optional soft inner
 * sheen. It can also be one row of an inset grouped list ({@link #setSegment}): only the outer
 * corners are rounded, the outline is drawn only on the group's outer edges and non-last rows get
 * an inset separator — so stacked rows look like one continuous iOS-style group.
 */
public class GlassCardView extends MaterialCardView {

    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint separatorPaint = new Paint();
    private final RectF rect = new RectF();

    private int segmentPosition = 0;
    private int segmentSize = 1;
    private float separatorInset;
    private boolean sheen;
    private float baseRadius;
    private int specularColor;
    private int sheenColor;
    private int lastShaderHeight = -1;

    public GlassCardView(@NonNull Context context) { this(context, null); }

    public GlassCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialCardViewStyle);
    }

    public GlassCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float d = getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassCardView);
        separatorInset = a.getDimension(R.styleable.GlassCardView_ieSeparatorInset, 16 * d);
        sheen = a.getBoolean(R.styleable.GlassCardView_ieSheen, false);
        a.recycle();

        float hair = Math.max(1f, 0.75f * d);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(hair);
        outlinePaint.setColor(ContextCompat.getColor(context, R.color.ie_glass_outline));
        specularPaint.setStyle(Paint.Style.STROKE);
        specularPaint.setStrokeWidth(Math.max(1f, d));
        specularColor = ContextCompat.getColor(context, R.color.ie_glass_specular);
        sheenColor = ContextCompat.getColor(context, R.color.ie_glass_sheen);
        separatorPaint.setColor(ContextCompat.getColor(context, R.color.ie_separator));
        separatorPaint.setStrokeWidth(Math.max(1f, Math.round(0.5f * d)));
        baseRadius = getRadius();
        setWillNotDraw(false);
    }

    /** Makes this card row {@code position} of a group of {@code size} rows. */
    public void setSegment(int position, int size) {
        if (position == segmentPosition && size == segmentSize) return;
        segmentPosition = position;
        segmentSize = Math.max(1, size);
        boolean first = position == 0, last = position >= segmentSize - 1;
        float r = baseRadius;
        ShapeAppearanceModel.Builder b = getShapeAppearanceModel().toBuilder()
                .setTopLeftCornerSize(first ? r : 0).setTopRightCornerSize(first ? r : 0)
                .setBottomLeftCornerSize(last ? r : 0).setBottomRightCornerSize(last ? r : 0);
        setShapeAppearanceModel(b.build());
        invalidate();
    }

    public void setSeparatorInset(float px) {
        separatorInset = px;
        invalidate();
    }

    public void setSheen(boolean on) {
        sheen = on;
        invalidate();
    }

    @Override
    public void setRadius(float radius) {
        super.setRadius(radius);
        baseRadius = radius;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        boolean first = segmentPosition == 0;
        boolean last = segmentPosition >= segmentSize - 1;
        float r = baseRadius;
        float half = outlinePaint.getStrokeWidth() / 2f;
        // Virtual rect: rows inside a group extend past their own bounds so only the group's outer
        // edges (and outer corners) are drawn; the canvas clip cuts the rest.
        rect.set(half, first ? half : -2 * r - 4, w - half, last ? h - half : h + 2 * r + 4);
        canvas.save();
        canvas.clipRect(0, 0, w, h);
        if (first && sheen) {
            sheenPaint.setShader(new LinearGradient(0, 0, 0, Math.min(h, r * 3),
                    sheenColor, sheenColor & 0x00FFFFFF, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, r, r, sheenPaint);
        }
        canvas.drawRoundRect(rect, r, r, outlinePaint);
        if (first) {
            if (lastShaderHeight != h) {
                float fade = Math.min(h, r * 1.6f);
                specularPaint.setShader(new LinearGradient(0, 0, 0, fade,
                        specularColor, specularColor & 0x00FFFFFF, Shader.TileMode.CLAMP));
                lastShaderHeight = h;
            }
            canvas.drawRoundRect(rect, r, r, specularPaint);
        }
        if (!last) {
            boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
            float y = h - separatorPaint.getStrokeWidth() / 2f;
            float x0 = rtl ? 0 : separatorInset, x1 = rtl ? w - separatorInset : w;
            canvas.drawLine(x0, y, x1, y, separatorPaint);
        }
        canvas.restore();
    }
}
