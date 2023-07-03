package kaiyuan.flappybird;

import java.awt.*;
import java.awt.image.BufferedImage;

import kaiyuan.flappybird.Constant;
import kaiyuan.flappybird.GameUtil;
import kaiyuan.flappybird.MusicUtil;
import kaiyuan.flappybird.Game;

public class Bird {

    public static final int IMG_COUNT = 8; // 图片数量
    public static final int STATE_COUNT = 4; // 状态数
    private final BufferedImage[][] birdImages; // 小鸟的图片数组对象

    private final int x;
    private int y; // 小鸟的坐标
    private int wingState; // 翅膀状态

    private BufferedImage image; // 实时的小鸟图片

    // 小鸟的状态
    private int state;
    public static final int BIRD_NORMAL = 0; //正常
    public static final int BIRD_UP = 1; //上升
    public static final int BIRD_FALL = 2; //下落
    public static final int BIRD_DEAD_FALL = 3; //死亡下落
    public static final int BIRD_DEAD = 4; //死亡

    private final Rectangle birdCollisionRect; // 碰撞矩形
    public static final int RECT_DESCALE = 2; // 补偿碰撞矩形宽高的参数

    private final ScoreCounter counter; // 计分器
    private final OverAnimation gameOverAnimation;

    public static int BIRD_WIDTH;
    public static int BIRD_HEIGHT;


    // 在构造器中对资源初始化
    public Bird() {
        counter = ScoreCounter.getInstance(); // 计分器
        gameOverAnimation = new OverAnimation();

        // 读取小鸟图片资源
        birdImages = new BufferedImage[STATE_COUNT][IMG_COUNT];
        for (int j = 0; j < STATE_COUNT; j++) {
            for (int i = 0; i < IMG_COUNT; i++) {
                birdImages[j][i] = GameUtil.loadBufferedImage(Constant.BIRDS_IMG_PATH[j][i]);
            }
        }

        assert birdImages[0][0] != null;
        BIRD_WIDTH = birdImages[0][0].getWidth();
        BIRD_HEIGHT = birdImages[0][0].getHeight();

        // 初始化小鸟的坐标
        x = Constant.FRAME_WIDTH >> 2;
        y = Constant.FRAME_HEIGHT >> 1;

        // 初始化碰撞矩形
        int rectX = x - BIRD_WIDTH / 2;
        int rectY = y - BIRD_HEIGHT / 2;
        birdCollisionRect = new Rectangle(rectX + RECT_DESCALE, rectY + RECT_DESCALE * 2, BIRD_WIDTH - RECT_DESCALE * 3,
                BIRD_WIDTH - RECT_DESCALE * 4); // 碰撞矩形的坐标与小鸟相同
    }

    //绘制方法
    public void draw(Graphics g) {
        movement();
        int state_index = Math.min(state, BIRD_DEAD_FALL); // 图片资源索引
        // 小鸟中心点计算
        int halfImgWidth = birdImages[state_index][0].getWidth() >> 1;
        int halfImgHeight = birdImages[state_index][0].getHeight() >> 1;
        if (velocity > 0)
            image = birdImages[BIRD_UP][0];
        g.drawImage(image, x - halfImgWidth, y - halfImgHeight, null); // x坐标于窗口1/4处，y坐标位窗口中心

        if (state == BIRD_DEAD)
            gameOverAnimation.draw(g, this);
        else if (state != BIRD_DEAD_FALL)
            drawScore(g);
        // 绘制碰撞矩形
//      g.setColor(Color.black);
//      g.drawRect((int) birdRect.getX(), (int) birdRect.getY(), (int) birdRect.getWidth(), (int) birdRect.getHeight());
    }

    public static final int ACC_FLAP = 14; // 玩家的速度
    public static final double ACC_Y = 2; // 玩家向下加速
    public static final int MAX_VEL_Y = 15; // 沿Y方向的最大水平，最大下降速度
    private int velocity = 0; // 鸟沿Y方向的速度，默认值与玩家相同
    private final int BOTTOM_BOUNDARY = Constant.FRAME_HEIGHT - Background.GROUND_HEIGHT - (BIRD_HEIGHT / 2);

    //鸟的飞行逻辑
    private void movement() {
        // 翅膀状态，实现小鸟扇动翅膀
        wingState++;
        image = birdImages[Math.min(state, BIRD_DEAD_FALL)][wingState / 10 % IMG_COUNT];
        if (state == BIRD_FALL || state == BIRD_DEAD_FALL) {
            freeFall();
            if (birdCollisionRect.y > BOTTOM_BOUNDARY) {
                if (state == BIRD_FALL) {
                    MusicUtil.playCrash();
                }
                die();
            }
        }
    }

    //自由落体
    private void freeFall() {
        if (velocity < MAX_VEL_Y)
            velocity -= ACC_Y;
        y = Math.min((y - velocity), BOTTOM_BOUNDARY);
        birdCollisionRect.y = birdCollisionRect.y - velocity;
    }

    //死亡
    public void die() {
        counter.saveScore();
        state = BIRD_DEAD;
        Game.setGameState(Game.STATE_OVER);
    }

    //扇动翅膀
    public void birdFlap() {
        if (keyIsReleased()) {
            if (isDead())
                return;
            MusicUtil.playFly(); // 播放音效
            state = BIRD_UP;
            if (birdCollisionRect.y > Constant.TOP_BAR_HEIGHT) {
                velocity = ACC_FLAP; // 每次振翅将速度改为上升速度
                wingState = 0; // 重置翅膀状态
            }
            keyPressed();
        }
    }

    //小鸟下降
    public void birdFall() {
        if (isDead())
            return;
        state = BIRD_FALL;
    }

    //小鸟在死亡之后下降
    public void deadBirdFall() {
        state = BIRD_DEAD_FALL;
        MusicUtil.playCrash(); // 播放音效
        velocity = 0;  // 速度置0，防止小鸟继续上升与水管重叠
    }

    //判断小鸟是否死亡
    public boolean isDead() {
        return state == BIRD_DEAD_FALL || state == BIRD_DEAD;
    }

    //绘制实时分数
    private void drawScore(Graphics g) {
        g.setColor(Color.white);
        g.setFont(Constant.CURRENT_SCORE_FONT);
        String str = Long.toString(counter.getCurrentScore());
        int x = Constant.FRAME_WIDTH - GameUtil.getStringWidth(Constant.CURRENT_SCORE_FONT, str) >> 1;
        g.drawString(str, x, Constant.FRAME_HEIGHT / 10);
    }

    // 重置小鸟
    public void reset() {
        state = BIRD_NORMAL; // 小鸟状态
        y = Constant.FRAME_HEIGHT >> 1; // 小鸟坐标
        velocity = 0; // 小鸟速度

        int ImgHeight = birdImages[state][0].getHeight();
        birdCollisionRect.y = y - ImgHeight / 2 + RECT_DESCALE * 2; // 小鸟碰撞矩形坐标

        counter.reset(); // 重置计分器
    }

    private boolean keyFlag = true; // 按键状态，true为已释放，使当按住按键时不会重复调用方法

    public void keyPressed() {
        keyFlag = false;
    }

    public void keyReleased() {
        keyFlag = true;
    }

    public boolean keyIsReleased() {
        return keyFlag;
    }

    public long getCurrentScore() {
        return counter.getCurrentScore();
    }

    public long getBestScore() {
        return counter.getBestScore();
    }

    public int getBirdX() {
        return x;
    }

    // 获取小鸟的碰撞矩形
    public Rectangle getBirdCollisionRect() {
        return birdCollisionRect;
    }


}