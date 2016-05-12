package com.deserteaglefe.game2048;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

/**
 * Function:
 * 重力传感器版2048——可以用 滑动(上下左右的手势) 或 传感器（旋转） 操纵的2048
 *
 * @author DesertEagleFe
 * @version 1.0
 *          <p>
 *          ps:
 *          因为上周的播放器雏形，摇一摇换音乐做起来简单，觉得不过瘾，遂做此游戏
 *          <p>
 *          pps:
 *          已经不记得去年自学java时，第一周就写完2048时的心情
 *          可惜当时的代码早已因为换电脑而遗失
 *          如今再写2048，却已物是人非，心情难以平静
 */
public class MainActivity extends AppCompatActivity {

    // 常量
    private static final String SCORE = "Score: ";
    private static final String LOST = "You've lost!";
    private static final String MAX_SCORE = "Max Score: ";
    private static final String MAX_SCORE_DATA = "max_score";
    public static final String PREFERENCE_NAME = "preference";

    // 视图
    private TextView maxScoreTextView;            // 最高分数
    private TextView scoreTextView;               // 当前分数
    private Button[][] blocks = new Button[4][4]; // 块
    private Button mResetButton;                  // 重置

    // 动画
    private Animator.AnimatorListener mAnimatorListener; // 动画监听器
    private float mBaseX;                   // 左上角块的x位置
    private float mBaseY;                   // 左上角块的y位置
    private float defaultDelta;             // 块间距
    private boolean isInitBase = false;     // 前面三个值初始化了吗？ps:不能再onCreate()或者onResume()中初始化
    private boolean isMoving = false;       // 是否在移动中？移动的时候不接受做其他动作
    private int mCounts = 0;                // 多少个需要移动的块
    private Button[] masks = new Button[12]; // 遮罩块，用于实现动画——障眼法

    // 数据
    private SharedPreferences mSharedPreferences;
    private int[][] data = new int[4][4];
    private int mScore;

    // 手势
    private GestureDetector mGestureDetector;
    private int mMaxScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        setListeners();
        init();
    }

    private void findViews() {
        maxScoreTextView = (TextView) findViewById(R.id.max_score_text);
        scoreTextView = (TextView) findViewById(R.id.score_text);
        blocks[0][0] = (Button) findViewById(R.id.block_0_0);
        blocks[0][1] = (Button) findViewById(R.id.block_0_1);
        blocks[0][2] = (Button) findViewById(R.id.block_0_2);
        blocks[0][3] = (Button) findViewById(R.id.block_0_3);
        blocks[1][0] = (Button) findViewById(R.id.block_1_0);
        blocks[1][1] = (Button) findViewById(R.id.block_1_1);
        blocks[1][2] = (Button) findViewById(R.id.block_1_2);
        blocks[1][3] = (Button) findViewById(R.id.block_1_3);
        blocks[2][0] = (Button) findViewById(R.id.block_2_0);
        blocks[2][1] = (Button) findViewById(R.id.block_2_1);
        blocks[2][2] = (Button) findViewById(R.id.block_2_2);
        blocks[2][3] = (Button) findViewById(R.id.block_2_3);
        blocks[3][0] = (Button) findViewById(R.id.block_3_0);
        blocks[3][1] = (Button) findViewById(R.id.block_3_1);
        blocks[3][2] = (Button) findViewById(R.id.block_3_2);
        blocks[3][3] = (Button) findViewById(R.id.block_3_3);
        mResetButton = (Button) findViewById(R.id.reset);
        masks[0] = (Button)findViewById(R.id.mask_0);
        masks[1] = (Button)findViewById(R.id.mask_1);
        masks[2] = (Button)findViewById(R.id.mask_2);
        masks[3] = (Button)findViewById(R.id.mask_3);
        masks[4] = (Button)findViewById(R.id.mask_4);
        masks[5] = (Button)findViewById(R.id.mask_5);
        masks[6] = (Button)findViewById(R.id.mask_6);
        masks[7] = (Button)findViewById(R.id.mask_7);
        masks[8] = (Button)findViewById(R.id.mask_8);
        masks[9] = (Button)findViewById(R.id.mask_9);
        masks[10] = (Button)findViewById(R.id.mask_10);
        masks[11] = (Button)findViewById(R.id.mask_11);


    }

    // 初始化程序
    private void init() {
        // 读取最高分
        mSharedPreferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        mMaxScore = mSharedPreferences.getInt(MAX_SCORE_DATA, 0);
        setMaxScore();

        // Button什么的只不过设置文字方便,点击效果就不要有了,会使动画不美观
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                blocks[i][j].setClickable(false);
            }
        }
        // 初始化游戏
        start();
    }

    // 初始化游戏：初始分数为0，有两个随机块
    private void start() {
        for (int i = 0; i < 16; i++) {
            data[i / 4][i % 4] = 0;
        }
        refresh();
        setBlockView(nextBlock(), randomNumber(), true);
        setBlockView(nextBlock(), randomNumber(), true);
        mScore = 0;
        setScore();
        isMoving = false;
    }

    // 随机新块
    private int nextBlock() {
        int next;
        Random random = new Random();
        do {
            next = random.nextInt(16);
        } while (data[next / 4][next % 4] != 0);
        return next;
    }

    // 新块的数字，10%概率为4，90%概率为2
    private int randomNumber() {
        Random random = new Random();
        int chance = random.nextInt(10);
        if (chance == 0) {
            return 4;
        } else {
            return 2;
        }
    }

    private void setMaxScore() {
        String maxScore = MAX_SCORE + mMaxScore;
        maxScoreTextView.setText(maxScore);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(MAX_SCORE_DATA, mMaxScore);
        editor.apply();
    }

    private void setScore() {
        String scoreText;
        if (isFull()) {
            scoreText = LOST + SCORE + mScore;
            scoreTextView.setTextColor(Color.rgb(255, 0, 0));
        } else {
            scoreText = SCORE + mScore;
            scoreTextView.setTextColor(Color.rgb(0, 0, 0));
        }
        scoreTextView.setText(scoreText);
        if (mScore > mMaxScore) {
            mMaxScore = mScore;
            setMaxScore();
        }
    }

    // 更新块视图
    private void setBlockView(int pos, int datum, boolean isSet) {
        int posX = pos / 4;
        int posY = pos % 4;
        if(isSet){
            data[posX][posY] = datum;
        }
        String text;
        // 有数字的块涂上明亮的颜色，没有的涂上暗的
        if (datum == 0) {
            text = "";
            blocks[posX][posY].setBackgroundColor(Color.rgb(170, 170, 170));
        } else {
            text = "" + datum;
            blocks[posX][posY].setBackgroundColor(Color.rgb(204, 204, 204));
        }
        // 自动适配字体
        if (text.length() <= 2) {
            blocks[posX][posY].setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
        } else {
            blocks[posX][posY].setTextSize(TypedValue.COMPLEX_UNIT_SP, 96 / text.length());
        }
        // 设置块的数字和颜色
        blocks[posX][posY].setText(text);
        blocks[posX][posY].setTextColor(getBlockColor(datum));
    }

    // 更新块视图：不用考虑0，移动结束会统一隐藏
    private void setMaskView(int pos, int datum) {
        String text = "" + datum;
        // 自动适配字体
        if (text.length() <= 2) {
            masks[pos].setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
        } else {
            masks[pos].setTextSize(TypedValue.COMPLEX_UNIT_SP, 96 / text.length());
        }
        // 设置块的数字
        masks[pos].setText(text);
        masks[pos].setTextColor(getBlockColor(datum));
        masks[pos].setVisibility(View.VISIBLE);
    }

    private void refresh() {
        for (int i = 0; i < 16; i++) {
            int posX = i / 4;
            int posY = i % 4;
            setBlockView(i, data[posX][posY], false);
        }
        for (int i = 0; i < 12; i++) {
            masks[i].setVisibility(View.INVISIBLE);
            masks[i].clearAnimation();
        }
        Log.i("TAG",data[0][0] + " " + data[1][0] + " " + data[2][0] + " " + data[3][0] + "\n" +
                data[0][1] + " " + data[1][1] + " " + data[2][1] + " " + data[3][1] + "\n" +
                data[0][2] + " " + data[1][2] + " " + data[2][2] + " " + data[3][2] + "\n" +
                data[0][3] + " " + data[1][3] + " " + data[2][3] + " " + data[3][3]);
    }

    public boolean isFull() {
        for (int a[] : data)
            for (int e : a)
                if (e == 0)
                    return false;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++) {
                if (data[i][j] == data[i][j + 1] || data[j][i] == data[j + 1][i])
                    return false;
            }
        return true;
    }

    // 木有看网上的方法，自己写的算法，应该有很多可以继续优化的地方
    // 以向右为例，针对每一行，大体上可以分为几种情况：
    // 最右面的不为零，那么向左搜寻到的下一个不为0的块，①要是两者数字相等，合并，加分②不相等，那么这个块会移动到右数第二个位置
    // ③最右面的为 0，那么从向左搜寻到的第一个不为0的块，在移动之后肯定会占据最右面的一个位置
    // 然后不用管最后一个了，剩下的区域重复上面的判断(迭代)
    // ps:其实好像有些可以合并。。。
    private void moveRight() {
        mCounts = 0;
        for (int posY = 0; posY < 4; posY++) {
            int toX = 3;
            for (int fromX = 3; fromX >= 0; fromX--) {
                if (data[fromX][posY] != 0 && fromX != toX) {
                    if (data[toX][posY] == 0) {
                        translationX(fromX, toX, posY, mCounts);
                        mCounts++;
                        data[toX][posY] = data[fromX][posY];
                        data[fromX][posY] = 0;
                    } else if (data[toX][posY] == data[fromX][posY]) {
                        translationX(fromX, toX, posY, mCounts);
                        mCounts++;
                        data[toX][posY] += data[fromX][posY];
                        mScore += data[toX][posY];
                        data[fromX][posY] = 0;
                        toX--;
                    } else {
                        toX--;
                        if (fromX != toX) {
                            translationX(fromX, toX, posY, mCounts);
                            mCounts++;
                            data[toX][posY] = data[fromX][posY];
                            data[fromX][posY] = 0;
                        }
                    }
                }
            }
        }
    }

    private void moveLeft() {
        mCounts = 0;
        for (int posY = 0; posY < 4; posY++) {
            int toX = 0;
            for (int fromX = 0; fromX < 4; fromX++) {
                if (data[fromX][posY] != 0 && fromX != toX) {
                    if (data[toX][posY] == 0) {
                        translationX(fromX, toX, posY, mCounts);
                        mCounts++;
                        data[toX][posY] = data[fromX][posY];
                        data[fromX][posY] = 0;
                    } else if (data[toX][posY] == data[fromX][posY]) {
                        translationX(fromX, toX, posY, mCounts);
                        mCounts++;
                        data[toX][posY] += data[fromX][posY];
                        mScore += data[toX][posY];
                        data[fromX][posY] = 0;
                        toX++;
                    } else {
                        toX++;
                        if (fromX != toX) {
                            translationX(fromX, toX, posY, mCounts);
                            mCounts++;
                            data[toX][posY] = data[fromX][posY];
                            data[fromX][posY] = 0;
                        }
                    }
                }
            }
        }
    }

    private void moveUp() {
        mCounts = 0;
        for (int posX = 0; posX < 4; posX++) {
            int toY = 0;
            for (int fromY = 0; fromY < 4; fromY++) {
                if (data[posX][fromY] != 0 && fromY != toY) {
                    if (data[posX][toY] == 0) {
                        translationY(posX, fromY, toY, mCounts);
                        mCounts++;
                        data[posX][toY] = data[posX][fromY];
                        data[posX][fromY] = 0;
                    } else if (data[posX][toY] == data[posX][fromY]) {
                        translationY(posX, fromY, toY, mCounts);
                        mCounts++;
                        data[posX][toY] += data[posX][fromY];
                        mScore += data[posX][toY];
                        data[posX][fromY] = 0;
                        toY++;
                    } else {
                        toY++;
                        if (fromY != toY) {
                            translationY(posX, fromY, toY, mCounts);
                            mCounts++;
                            data[posX][toY] = data[posX][fromY];
                            data[posX][fromY] = 0;
                        }
                    }
                }
            }
        }
    }

    private void moveDown() {
        mCounts = 0;
        for (int posX = 0; posX < 4; posX++) {
            int toY = 3;
            for (int fromY = 3; fromY >= 0; fromY--) {
                if (data[posX][fromY] != 0 && fromY != toY) {
                    if (data[posX][toY] == 0) {
                        translationY(posX, fromY, toY, mCounts);
                        mCounts++;
                        data[posX][toY] = data[posX][fromY];
                        data[posX][fromY] = 0;
                    } else if (data[posX][toY] == data[posX][fromY]) {
                        translationY(posX, fromY, toY, mCounts);
                        mCounts++;
                        data[posX][toY] += data[posX][fromY];
                        mScore += data[posX][toY];
                        data[posX][fromY] = 0;
                        toY--;
                    } else {
                        toY--;
                        if (fromY != toY) {
                            translationY(posX, fromY, toY, mCounts);
                            mCounts++;
                            data[posX][toY] = data[posX][fromY];
                            data[posX][fromY] = 0;
                        }
                    }
                }
            }
        }
    }

    private void translationX(int fromX, int toX, int posY, int count) {
        // getX()、getY在onCreate()、onResume()中调用都只能返回0.0
        if(!isInitBase){
            defaultDelta = blocks[1][0].getX() - blocks[0][0].getX();
            mBaseX = blocks[0][0].getX();
            mBaseY = blocks[0][0].getY();
        }
        isMoving = true; // 移动中...
        // 设定要移动的遮罩的起始状态
        masks[count].setX(mBaseX);
        masks[count].setY(mBaseY + posY * defaultDelta);
        setMaskView(count, data[fromX][posY]);
        ObjectAnimator animator = ObjectAnimator.ofFloat(masks[count], "translationX", fromX * defaultDelta, toX * defaultDelta);
        animator.setDuration(200);
        animator.start();
        animator.addListener(mAnimatorListener);
        setBlockView(fromX * 4 + posY, 0, false);
    }

    private void translationY(int posX, int fromY, int toY, int count) {
        // getX()、getY在onCreate()、onResume()中调用都只能返回0.0
        if(!isInitBase){
            defaultDelta = blocks[1][0].getX() - blocks[0][0].getX();
            mBaseX = blocks[0][0].getX();
            mBaseY = blocks[0][0].getY();
        }
        isMoving = true; // 移动中...
        // 设定要移动的遮罩的起始状态
        masks[count].setX(mBaseX + posX * defaultDelta);
        masks[count].setY(mBaseY);
        setMaskView(count, data[posX][fromY]);
        ObjectAnimator animator = ObjectAnimator.ofFloat(masks[count], "translationY", fromY * defaultDelta, toY * defaultDelta);
        animator.setDuration(200);
        animator.start();
        animator.addListener(mAnimatorListener);
        setBlockView(posX * 4 + fromY, 0, false);
    }

    // 上个背景色吧
    private int getBlockColor(int datum) {
        switch (datum) {
            case 2:
                return Color.rgb(0, 128, 128);
            case 4:
                return Color.rgb(128, 0, 128);
            case 8:
                return Color.rgb(255, 128, 200);
            case 16:
                return Color.rgb(255, 172, 128);
            case 32:
                return Color.rgb(255, 160, 160);
            case 64:
                return Color.rgb(255, 128, 128);
            case 128:
                return Color.rgb(255, 96, 96);
            case 256:
                return Color.rgb(255, 64, 64);
            case 512:
                return Color.rgb(255, 32, 32);
            case 1024:
                return Color.rgb(255, 0, 0);
            case 2048:
                return Color.rgb(128, 0, 0);
            default:
                return Color.rgb(0, 0, 0);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void setListeners() {
        mAnimatorListener = new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mCounts--;
                if (mCounts == 0) {
                    setBlockView(nextBlock(), randomNumber(), true);
                    setScore();
                    refresh();
                    isMoving = false;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };

        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
        GestureDetector.OnGestureListener onGestureListener = new GestureDetector.OnGestureListener() {
            private int verticalMinDistance = 20;

            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (!isMoving) {
                    float delta_x = e2.getX() - e1.getX();
                    float delta_y = e2.getY() - e1.getY();
                    if (Math.abs(delta_x) > verticalMinDistance || Math.abs(delta_y) > verticalMinDistance) {
                        if (Math.abs(delta_x) > Math.abs(delta_y)) {
                            if (delta_x > 0) {
                                moveRight();
                            } else {
                                moveLeft();
                            }
                        } else {
                            if (delta_y > 0) {
                                moveDown();
                            } else {
                                moveUp();
                            }
                        }
                    }
                }
                return false;
            }
        };
        mGestureDetector = new GestureDetector(this, onGestureListener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mGestureDetector.onTouchEvent(ev); // 不加的话会被子空间拦截从而只能在空白区域滑动
        return super.dispatchTouchEvent(ev);
    }
}
