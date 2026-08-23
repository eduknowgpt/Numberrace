package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ScaleUtils.i;
import static org.unicog.numberrace.util.Constants.BOARDANIM_LAYER;
import static org.unicog.numberrace.util.Constants.BOARD_LAYER;
import static org.unicog.numberrace.util.Constants.BOTTOM_LAYER;
import static org.unicog.numberrace.util.Constants.DOT_CONTAINER_LAYER;
import static org.unicog.numberrace.util.Constants.LAST_SQUARE;
import static org.unicog.numberrace.util.Constants.LEFT;
import static org.unicog.numberrace.util.Constants.RIGHT;
import static org.unicog.numberrace.vars.GraphicsVariables.OFFSET_X;
import static org.unicog.numberrace.vars.GraphicsVariables.OFFSET_Y;
import static org.unicog.numberrace.vars.GraphicsVariables.DISPLAY_WIDTH;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.sprites.BubbleSprite;
import org.unicog.numberrace.sprites.DotContainerSprite;
import org.unicog.numberrace.sprites.ImageButtonSprite;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.GraphicsVariables;

import com.threerings.media.FrameManager;
import com.threerings.media.FrameParticipant;
import com.threerings.media.ViewTracker;
import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.animation.Animation;
import com.threerings.media.animation.AnimationObserver;
import com.threerings.media.animation.ScaleAnimation;
import com.threerings.media.image.BufferedMirage;
import com.threerings.media.image.Mirage;
import com.threerings.media.sprite.ImageSprite;
import com.threerings.media.sprite.PathObserver;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.util.LinePath;
import com.threerings.media.util.MultiFrameImageImpl;
import com.threerings.media.util.Path;
import com.threerings.media.util.Pathable;
import com.threerings.util.DirectionCodes;
import java.awt.Font;
import org.unicog.numberrace.vars.ThemeVariables;

public class GameArea implements FrameParticipant, ViewTracker {

    private static final int PLATFORM_HEIGHT = 40;
    private static final int BOTTOM_Y = 715;
    private GameObject go;
    private ImageSprite[] squares = new ImageSprite[Constants.LAST_SQUARE];
    private ImageSprite[] bottomSquares = new ImageSprite[Constants.LAST_SQUARE];
    private ImageSprite[] platforms = new ImageSprite[Constants.LAST_SQUARE];
    private Point[] squareCenters = new Point[Constants.LAST_SQUARE];
    private Point[] bottomSquaresPos = new Point[Constants.LAST_SQUARE];
    private BufferedMirage squareMirage;
    private BufferedMirage platformMirage;
    public Path _scrollPath;
    public boolean board;
    private ScreenScroller screenScroller;
    private long _scrollPathStamp;
    private ImageSprite startMarker;
    private ImageSprite finishMarker;
    private DotContainerSprite[] dotContainers = new DotContainerSprite[2];
    private final VirtualMediaPanel vmp;
    public PathObserver scrollObserver;
    private final FrameManager frameManager;

    private Rectangle boardBounds;

    private int bottomSquareWidth;
    private int bottomMargin;
    private MouseAdapter bottomMouseListener;
    private int hnum = -1;
    private int dnum_i = -1;
    private int dnum_j = -1;

    private BubbleSprite bubbleSprite;

    public void setDnum(int from, int to) {
        if (dnum_j > 0) {
            for (int n = dnum_i > 0 ? dnum_i : 1; n <= dnum_j
                    && n <= squares.length; n++) {
                squares[n - 1].invalidate();
            }
        }

        this.dnum_i = Math.min(from, to);
        this.dnum_j = Math.max(from, to);

        if (dnum_j > 0) {
            for (int n = dnum_i > 0 ? dnum_i : 1; n <= dnum_j
                    && n <= squares.length; n++) {
                squares[n - 1].invalidate();
            }
        }
    }

    //    private ImageSprite bottomSprite;

    private final class Square extends ImageButtonSprite {
        final private String txt;

        boolean anim_running = false;

        private int num;

        private Square(Mirage image, String cmd, int num) {
            super(image, cmd, num);
            txt = Integer.toString(num);
            this.num = num;
        }

        private ScaleAnimation getZoomIn() {
            ScaleAnimation zoomIn = new ScaleAnimation(squareMirage, new Point(
                    (int) getBounds().getCenterX(),
                    (int) getBounds().getCenterY()), 1f, 1.3f, 300);
            zoomIn.setRenderOrder(BOARDANIM_LAYER);
            zoomIn.addAnimationObserver(new AnimationObserver() {

                public void animationCompleted(Animation anim, long when) {
                    addAnimation(getZoomOut());
                }

                public void animationStarted(Animation anim, long when) {
                    anim_running = true;
                }

            });
            return zoomIn;
        }

        private ScaleAnimation getZoomOut() {
            ScaleAnimation zoomOut = new ScaleAnimation(squareMirage,
                    new Point((int) getBounds().getCenterX(),
                            (int) getBounds().getCenterY()), 1.3f, 1f, 300);
            zoomOut.setRenderOrder(BOARDANIM_LAYER);
            zoomOut.addAnimationObserver(new AnimationObserver() {

                public void animationStarted(Animation anim, long when) {
                }

                public void animationCompleted(Animation anim, long when) {
                    anim_running = false;
                }
            });
            return zoomOut;
        }

        @Override
        public void setHovered(boolean hovered) {
            super.setHovered(hovered);
            //            if (hovered && !anim_running) {
            //                //                Animation anim = new ScaleAnimation(squareMirage, new Point(
            //                //                        (int) getBounds().getCenterX(),
            //                //                        (int) getBounds().getCenterY()), 0f, 1f, 300);
            //                //                anim.setRenderOrder(BOARDANIM_LAYER);
            //                //                addAnimation(anim);
            //                addAnimation(getZoomIn());
            //            }
        }

        public void paint(Graphics2D gfx) {

            if (!anim_running) {
                super.paint(gfx);
            }
            gfx.setFont(go.getTheme().boardNumberFont);

            if (num == hnum) {
                gfx.setColor(Color.RED);
            } else if (num > dnum_i && num <= dnum_j) {
                gfx.setColor(Color.YELLOW);
            } else if ((num % 10) == 0) {
                gfx.setColor(Color.BLACK);
            } else {
                gfx.setColor(Color.WHITE);
            }
            Utilities.drawFromPoint(txt,
                                    _bounds.x + _bounds.width / 2,
                                    _bounds.y + _bounds.height / 2,
                                    0,
                                    0,
                                    gfx);
        }

    }

    private final class ScreenScroller implements Pathable {

        public Rectangle getBounds() {
            return null;
        }

        public int getOrientation() {
            return DirectionCodes.NONE;
        }

        public int getX() {
            return vmp.getViewBounds().x;
        }

        public int getY() {
            return vmp.getViewBounds().y;
        }

        public void pathBeginning() {
        }

        public void pathCompleted(long timestamp) {
            Path oldpath = _scrollPath;
            _scrollPath = null;
            oldpath.wasRemoved(this);
            board = !board;
            if (scrollObserver != null) {
                scrollObserver.pathCompleted(null, oldpath, timestamp);
            }
        }

        public void setLocation(int x, int y) {
            vmp.setViewLocation(x, y);
        }

        public void setOrientation(int orient) {
        }

    }

    public GameArea(FrameManager frameManager, final VirtualMediaPanel parent) {
        this.frameManager = frameManager;
        this.vmp = parent;

        go = GameObject.getInstance();
        squareMirage = new BufferedMirage(getSquareImage());
        platformMirage = new BufferedMirage(getPlatformImage());
        screenScroller = new ScreenScroller();

    }

    private BufferedImage getPlatformImage() {
        int sqWidth = i(GraphicsVariables.NUMBER_BOARD_SQUARE_WIDTH);
        int plHeight = i(PLATFORM_HEIGHT);

        BufferedImage bufferedImage = new BufferedImage(sqWidth + 2, plHeight,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g2D = bufferedImage.createGraphics();

        g2D.addRenderingHints(Utilities.antialiasRH);
        g2D.setColor(ThemeVariables.boardColor);
        g2D.fillRect(1, 1, sqWidth - 1, plHeight - 1);

        g2D.setStroke(new BasicStroke(GraphicsVariables.NUMBER_BOARD_LINE_WIDTH));
        g2D.setColor(go.getTheme().boardLineColor);
        g2D.drawPolyline(new int[] { 0, 0, sqWidth, sqWidth }, new int[] {
                plHeight - 1, 0, 0, plHeight - 1 }, 4);

        g2D.dispose();
        return bufferedImage;
    }

    /**
     * Creates image representing square of our board
     *
     * @return created BufferedImage
     */
    private BufferedImage getSquareImage() {
        int sqWidth = i(GraphicsVariables.NUMBER_BOARD_SQUARE_WIDTH);

        BufferedImage bufferedImage = new BufferedImage(sqWidth
                + i(GraphicsVariables.NUMBER_BOARD_ARROW_SIZE) + 2, sqWidth,
                BufferedImage.TYPE_INT_ARGB);

        int arrowSize = i(GraphicsVariables.NUMBER_BOARD_ARROW_SIZE);
        int rightSide = sqWidth + arrowSize;
        int xPolyPoints[] = { 1, sqWidth, rightSide, sqWidth, 1, arrowSize, 1 };

        int yPolyPoints[] = { 1, 1, sqWidth >> 1, sqWidth - 1, sqWidth - 1,
                sqWidth >> 1, 1 };

        Graphics2D g2D = bufferedImage.createGraphics();

        g2D.addRenderingHints(Utilities.antialiasRH);
        g2D.setStroke(new BasicStroke(GraphicsVariables.NUMBER_BOARD_LINE_WIDTH));
        g2D.setColor(ThemeVariables.boardColor);
        g2D.fillPolygon(xPolyPoints, yPolyPoints, xPolyPoints.length);
        g2D.setColor(go.getTheme().boardLineColor);
        g2D.drawPolygon(xPolyPoints, yPolyPoints, xPolyPoints.length);

        g2D.dispose();
        return bufferedImage;
    }

    public void load() {
        // Dot Containers Part
        //bottomSquareWidth = i(960) / LAST_SQUARE;
        // changed to 40
        bottomSquareWidth = i(960) / 40;
        //bottomMargin = (i(36) + bottomSquareWidth) >> 1;
        // changed to support different squere counts
        //bottomMargin = (i((36*(40/LAST_SQUARE))) + bottomSquareWidth) >> 1;
        bottomMargin = (i(GraphicsVariables.DISPLAY_WIDTH) - (bottomSquareWidth * Constants.LAST_SQUARE)) / 2;

        DotContainerSprite leftDotContainer = new DotContainerSprite(LEFT,
                i(GraphicsVariables.DOT_CONT_WIDTH),
                i(GraphicsVariables.DOT_CONT_HEIGHT),
                i(GraphicsVariables.LEFT_DOT_CONT_IMG_X),
                i(GraphicsVariables.LEFT_DOT_CONT_IMG_Y), "choiceIS", LEFT);

        DotContainerSprite rightDotContainer = new DotContainerSprite(RIGHT,
                i(GraphicsVariables.DOT_CONT_WIDTH),
                i(GraphicsVariables.DOT_CONT_HEIGHT),
                i(GraphicsVariables.RIGHT_DOT_CONT_IMG_X),
                i(GraphicsVariables.RIGHT_DOT_CONT_IMG_Y), "choiceIS", RIGHT);

        dotContainers[Constants.LEFT] = leftDotContainer;
        dotContainers[Constants.RIGHT] = rightDotContainer;

        leftDotContainer.setFrames(new MultiFrameImageImpl(
                new Mirage[] {
                        new BufferedMirage(
                                ImageFactory.getImage(go.getTheme().leftDotContainerGifs[0])),
                        new BufferedMirage(
                                ImageFactory.getImage(go.getTheme().leftDotContainerGifs[1])) }));

        rightDotContainer.setFrames(new MultiFrameImageImpl(
                new Mirage[] {
                        new BufferedMirage(
                                ImageFactory.getImage(go.getTheme().rightDotContainerGifs[0])),
                        new BufferedMirage(
                                ImageFactory.getImage(go.getTheme().rightDotContainerGifs[1])) }));

        leftDotContainer.setLocation(i(OFFSET_X), i(OFFSET_Y));
        rightDotContainer.setLocation(i(DISPLAY_WIDTH)
                                              - rightDotContainer.getWidth()
                                              - 1 - i(OFFSET_X),
                                      i(OFFSET_Y));

        leftDotContainer.setRenderOrder(DOT_CONTAINER_LAYER);
        rightDotContainer.setRenderOrder(DOT_CONTAINER_LAYER);

        leftDotContainer.setBackground(go.getTheme().dotContColor);
        rightDotContainer.setBackground(go.getTheme().dotContColor);
        leftDotContainer.setDotContDigitColor(go.getTheme().dotContDigitColor);
        rightDotContainer.setDotContDigitColor(go.getTheme().dotContDigitColor);
        leftDotContainer.setDotContDigitFont(go.getTheme().dotContDigitFont);
        rightDotContainer.setDotContDigitFont(go.getTheme().dotContDigitFont);

        float coef = 1.f + (float) GraphicsVariables.NUMBER_BOARD_ARROW_SIZE
                / (float) GraphicsVariables.NUMBER_BOARD_SQUARE_WIDTH;

        System.out.println(coef);
        // Board Part
        for (int i = 0; i < squares.length; i++) {
            squares[i] = new Square(squareMirage, "sqClicked", i + 1);
            platforms[i] = new ImageButtonSprite(platformMirage, "sqClicked",
                    i + 1);
            final int x = i(GraphicsVariables.NUMBER_BOARD_SQUARE_WIDTH) + i
                    * i(GraphicsVariables.NUMBER_BOARD_SQUARE_WIDTH);
            final int y = i(GraphicsVariables.SQUARES_Y);

            bottomSquares[i] = createSpriteForBottom(squares[i], coef);
            bottomSquares[i].setLocation(bottomMargin + bottomSquareWidth * i,
                                         i(BOTTOM_Y)
                                                 + bottomSquares[i].getHeight());
            bottomSquares[i].setRenderOrder(BOTTOM_LAYER);

            squares[i].setLocation(x, y);
            squares[i].setRenderOrder(BOARD_LAYER);

            platforms[i].setLocation(x, y - platformMirage.getHeight());
            platforms[i].setRenderOrder(BOARD_LAYER);
        }

        bubbleSprite = new BubbleSprite(squareMirage.getWidth(),
                squareMirage.getHeight() / 2);

        BufferedImage image = ImageFactory.getImage(go.getTheme().startMarker);
        startMarker = createSpriteForBottom(new ImageSprite(new BufferedMirage(
                image)), 1.5);

        image = ImageFactory.getImage(go.getTheme().finishMarker);
        finishMarker = createSpriteForBottom(new ImageSprite(
                new BufferedMirage(image)), 1.5);

        bottomMouseListener = new MouseAdapter() {

            private int y = i(BOTTOM_Y);
            private boolean bottomPressed;

            @Override
            public void mousePressed(MouseEvent e) {
                bottomPressed = e.getY() > y;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (bottomPressed && e.getY() > y) {
                    switchArea((e.getX() - vmp.getViewBounds().x - bottomMargin)
                                       / bottomSquareWidth,
                               null);
                }
                bottomPressed = false;
            }
        };

        //        bottomSprite = new ImageSprite(new BufferedMirage(getBottomImage()));
        //        bottomSprite.setRenderOrder(Constants.BOTTOM_LAYER);
        frameManager.registerFrameParticipant(this);
    }

    public void start() {
        //        ((NRRegionManager)_remgr).setup(parentRegionManager, this, getParent());

        for (int i = 0; i < squares.length; i++) {
            platforms[i].layout();
            Rectangle sqBounds = platforms[i].getBounds();
            squareCenters[i] = new Point((int) sqBounds.getCenterX(),
                    (int) sqBounds.getCenterY());
            bottomSquaresPos[i] = new Point(bottomMargin + bottomSquareWidth
                    * i, i(BOTTOM_Y));
        }
        final int bottomY = bottomSquares[0].getY()
                + bottomSquares[0].getHeight();
        startMarker.setLocation(bottomMargin - startMarker.getWidth(), bottomY
                - startMarker.getHeight());
        finishMarker.setLocation(bottomMargin + bottomSquareWidth * LAST_SQUARE,
                                 bottomY - finishMarker.getHeight());

        for (int i = 0; i < squares.length; i++) {
            addSprite(squares[i]);
            addSprite(platforms[i]);
            addSprite(bottomSquares[i]);
        }

        addSprite(startMarker);
        addSprite(finishMarker);

        for (int i = 0; i < dotContainers.length; i++) {
            addSprite(dotContainers[i]);
        }

        boardBounds = new Rectangle(squares[0].getX(), platforms[0].getY(),
                squares[squares.length - 1].getX()
                        + squares[squares.length - 1].getWidth()
                        - squares[0].getX() + 1, platforms[0].getHeight()
                        + squares[0].getHeight());
        //        boardBounds = new Rectangle(-squares[0].getWidth(),
        //                platforms[0].getY(), squares[squares.length - 1].getX()
        //                        + squares[squares.length - 1].getWidth() * 3,
        //                platforms[0].getHeight() + squares[0].getHeight());

        //        bottomSprite.layout();
        //        final Rectangle viewBounds = vmp.getViewBounds();
        //        bottomSprite.setLocation(viewBounds.x + bottomMargin, viewBounds.y
        //                + viewBounds.height - bottomSprite.getHeight() - 1);
        //        addSprite(bottomSprite);

        vmp.addMouseListener(bottomMouseListener);

        vmp.addViewTracker(this);
    }

    public void stop() {
        vmp.removeMouseListener(bottomMouseListener);
        vmp.removeViewTracker(this);
        for (int i = 0; i < squares.length; i++) {
            removeSprite(platforms[i]);
            removeSprite(squares[i]);
            removeSprite(bottomSquares[i]);
        }
        removeSprite(startMarker);
        removeSprite(finishMarker);

        for (int i = 0; i < dotContainers.length; i++) {
            removeSprite(dotContainers[i]);
        }
        //        removeSprite(bottomSprite);
    }

    /**
     * Ticks any scroll path assigned to this VirtualMediaPanel.
     *
     * @return true if the path scrolled the panel as a result of this tick,
     *         false if it remained in the same position.
     */
    protected boolean tickScrollPath(long tickStamp) {
        if (_scrollPath == null) {
            return false;
        }

        // initialize the path if we haven't yet
        if (_scrollPathStamp == 0) {
            _scrollPath.init(screenScroller, _scrollPathStamp = tickStamp);
        }

        // it's possible that as a result of init() the path completed and
        // removed itself with a call to pathCompleted(), so we have to be
        // careful here
        return (_scrollPath == null) ? true : _scrollPath.tick(screenScroller,
                                                               tickStamp);
    }

    /**
     * Returns true if this panel is currently scrolling, false if it is not.
     */
    public boolean isScrolling() {
        return (_scrollPath != null);
    }

    protected void scroll(Path path) {
        // if there's a previous path, let it know that it's going away
        cancelScroll();

        // save off this path
        _scrollPath = path;

        // we'll initialize it on our next tick thanks to a zero path stamp
        _scrollPathStamp = 0;
    }

    /**
     * Cancels any path that the sprite may currently be moving along.
     */
    protected void cancelScroll() {
        if (_scrollPath != null) {
            _scrollPath = null;
        }
    }

    //    /**
    //     * Returns the path being followed by this sprite or null if the sprite is
    //     * not following a path.
    //     */
    //    public Path getPath() {
    //        return _path;
    //    }

    public void switchArea(int i, PathObserver observer) {

        if (isScrolling()) {
            return;
        }

        scrollObserver = observer;

        Rectangle _vbounds = vmp.getViewBounds();

        int whereX = i > 0 ? (int) squareCenters[i - 1].getX()
                : squares[0].getWidth();
        int scroll = whereX - (_vbounds.x + squares[0].getWidth());

        Rectangle lsbounds = squares[squares.length - 1].getBounds();
        final int firstInvisibleX = _vbounds.x + _vbounds.width;
        final int xAfterLastSquare = lsbounds.x + lsbounds.width;
        if ((firstInvisibleX + scroll) > xAfterLastSquare) {
            scroll = (xAfterLastSquare - firstInvisibleX);
        }
        scroll(new LinePath(_vbounds.x, _vbounds.y, _vbounds.x + scroll,
                _vbounds.y, 500));
    }

    public DotContainerSprite getLeftDotContainer() {
        return getDotContainer(Constants.LEFT);
    }

    public DotContainerSprite getRightDotContainer() {
        return getDotContainer(Constants.RIGHT);
    }

    private void addSprite(Sprite sprite) {
        vmp.addSprite(sprite);
    }

    private void removeSprite(Sprite sprite) {
        vmp.removeSprite(sprite);
    }

    private void addAnimation(Animation animation) {
        vmp.addAnimation(animation);
    }

    public Component getComponent() {
        return null;
    }

    public boolean needsPaint() {
        return false;
    }

    public void tick(long tickStamp) {
        tickScrollPath(tickStamp);
    }

    public Point getSquareCenter(int i) {
        return squareCenters[i - 1];
    }

    public void resetContainers() {
        for (int i = 0; i < dotContainers.length; i++) {
            dotContainers[i].reset();
        }
    }

    public void unload() {
        frameManager.removeFrameParticipant(this);
        startMarker = null;
        finishMarker = null;
        bottomMouseListener = null;
        bubbleSprite = null;
        for (int i = 0; i < dotContainers.length; i++) {
            dotContainers[i] = null;
        }
        for (int i = 0; i < squares.length; i++) {
            squares[i] = null;
        }

        for (int i = 0; i < bottomSquaresPos.length; i++) {
            bottomSquaresPos[i] = null;
        }
    }

    public void viewLocationDidChange(int dx, int dy) {
        for (int i = 0; i < bottomSquaresPos.length; i++) {
            bottomSquaresPos[i].translate(dx, dy);
        }
    }

    public Rectangle getBoardBounds() {
        return boardBounds;
    }

    final public DotContainerSprite getDotContainer(int side) {
        return dotContainers[side];
    }

    public void align(Sprite stamp, int side) {
        stamp.setRenderOrder(Constants.DOT_CONTAINER_LAYER + 1);

        Rectangle sBounds = stamp.getBounds();
        Rectangle cBounds = getDotContainer(side).getBounds();
        int y = cBounds.y + cBounds.height - (int) (sBounds.height * 1.25);
        if (side == LEFT) {
            stamp.setLocation((int) (cBounds.x + cBounds.width - sBounds.width * 1.25),
                              y);

        } else if (side == RIGHT) {
            stamp.setLocation(cBounds.x + (sBounds.width >> 2), y);
        }
    }

    public ImageSprite createSpriteForBottom(ImageSprite sprite) {
        return createSpriteForBottom(sprite, 1.);
    }

    public ImageSprite createSpriteForBottom(ImageSprite sprite, double scale) {

        sprite.layout();
        Rectangle sb = sprite.getBounds();
        final int targetWidth = (int) (bottomSquareWidth * scale);
        double scaleFactor = targetWidth / sb.getWidth();

        BufferedImage img = ImageFactory.createCompatibleTranslucentImage(sb.width,
                                                                          sb.height);

        Graphics2D g2d = img.createGraphics();
        g2d.addRenderingHints(Utilities.antialiasRH);
        g2d.translate(-sb.x, -sb.y);
        sprite.paint(g2d);
        g2d.dispose();
        BufferedImage miniImage = ImageFactory.getFasterScaledInstance(img,
                                                                       targetWidth,
                                                                       (int) (sb.height * scaleFactor),
                                                                       RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                                                                       true);
        ImageSprite miniSprite = new ImageButtonSprite(new BufferedMirage(
                miniImage), null, null);
        miniSprite.setRenderOrder(BOTTOM_LAYER + 1);
        miniSprite.layout();
        return miniSprite;
    }

    public Point calculatePositionATBottom(Sprite sprite, int boardPosition) {
        final Point p = getBottomPosition(boardPosition);
        final int sWidth = sprite.getWidth();
        if (sWidth > bottomSquareWidth) {
            p.translate(-((sWidth - bottomSquareWidth) >>> 1),
                        (sWidth > bottomSquareWidth) ? -(sprite.getHeight() >> 1)
                                : 0);
        }

        return p;
    }

    private Point getBottomPosition(int boardPosition) {
        if (boardPosition < 1) {
            return new Point(vmp.getViewBounds().x + bottomMargin
                    - startMarker.getWidth(), vmp.getViewBounds().y
                    + i(BOTTOM_Y));
        }
        return new Point(bottomSquaresPos[boardPosition - 1]);
    }

    public void setHnum(int boardPosition) {
        if (hnum > 0 && hnum <= squares.length) {
            squares[hnum - 1].invalidate();
        }
        this.hnum = boardPosition;
        if (hnum > 0 && hnum <= squares.length) {
            squares[hnum - 1].invalidate();
        }
    }

    public void setHint(int where, String what) {
        if (where > 0 && where <= squares.length) { // we need to have square corresponded to where - 1
            Rectangle bounds = squares[where - 1].getBounds();
            bubbleSprite.setLocation(bounds.x, bounds.y + bounds.height);
            bubbleSprite.setLabel(what);
            addSprite(bubbleSprite);
        } else {
            removeSprite(bubbleSprite);
        }
    }
}
