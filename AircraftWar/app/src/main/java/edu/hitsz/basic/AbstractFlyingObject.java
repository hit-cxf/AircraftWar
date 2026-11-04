package edu.hitsz.basic;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.application.ImageManager;

import javafx.scene.image.Image;

/**
 * 可飞行对象的父类
 *
 * @author hitsz
 */
public abstract class AbstractFlyingObject {

    //locationX,locationY为图片中心位置坐标
    protected int locationX;
    protected int locationY;

    // x,y轴移动速度
    protected int speedX;
    protected int speedY;
    private double velocityX = Double.NaN;
    private double velocityY = Double.NaN;

    protected final MovementEffect movementEffect = new MovementEffect();
    private double remainingX;
    private double remainingY;

    //图片, null 表示未设置
    protected Image image = null;

    //x 轴长度，根据图片尺寸获得, -1 表示未设置
    protected int width = -1;

    //y 轴长度，根据图片尺寸获得, -1 表示未设置
    protected int height = -1;

    //有效（生存）标记，标记为 false的对象会在下次刷新时清除
    protected boolean isValid = true;

    public AbstractFlyingObject() {
    }

    public AbstractFlyingObject(int locationX, int locationY, int speedX, int speedY) {
        this.locationX = locationX;
        this.locationY = locationY;
        this.speedX = speedX;
        this.speedY = speedY;
    }

    /**
     * 根据原始速度和临时效果移动，保留不足一个像素的位移。
     */
    public void forward() {
        double scale = movementEffect.scale();
        if (scale == 0.0) {
            return;
        }
        remainingX += effectiveVelocity(velocityX, speedX) * scale;
        remainingY += effectiveVelocity(velocityY, speedY) * scale;
        int dx = (int) (remainingX + Math.copySign(1e-9, remainingX));
        int dy = (int) (remainingY + Math.copySign(1e-9, remainingY));
        locationX += dx;
        locationY += dy;
        remainingX -= dx;
        remainingY -= dy;
    }

    /** 配置允许小数速度，边界反弹仍沿用子类维护的整数方向。 */
    public final void setVelocity(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("移动速度必须是有限数值");
        }
        velocityX = x;
        velocityY = y;
        speedX = directionMarker(x);
        speedY = directionMarker(y);
    }

    private static int directionMarker(double speed) {
        return speed == 0 ? 0 : (int) Math.copySign(Math.max(1, Math.round(Math.abs(speed))), speed);
    }

    private static double effectiveVelocity(double precise, int direction) {
        return Double.isNaN(precise) ? direction : Math.copySign(Math.abs(precise), direction);
    }

    public final double getVelocityX() { return effectiveVelocity(velocityX, speedX); }

    public final double getVelocityY() { return effectiveVelocity(velocityY, speedY); }

    /** 暂停/继续本对象的冻结和减速时限，避免菜单暂停消耗道具效果。 */
    public final void setEffectsPaused(boolean paused) {
        if (paused) {
            movementEffect.pause();
        } else {
            movementEffect.resume();
        }
    }

    protected final boolean isMovementFrozen() {
        return movementEffect.scale() == 0.0;
    }

    /** 渲染冰冻/减速状态，显示时限与真实移动效果共用同一状态。 */
    public final boolean hasMovementEffect() {
        return movementEffect.scale() < 1.0;
    }

    /**
     * 碰撞检测，当对方坐标进入我方范围，判定我方击中<br>
     * 对方与我方覆盖区域有交叉即判定撞击。
     *  <br>
     * 非飞机对象区域：
     *  横向，[x - width/2, x + width/2]
     *  纵向，[y - height/2, y + height/2]
     *  <br>
     * 飞机对象区域：
     *  横向，[x - width/2, x + width/2]
     *  纵向，[y - height/4, y + height/4]
     *
     * @param flyingObject 撞击对方
     * @return true: 我方被击中; false 我方未被击中
     */
    public boolean crash(AbstractFlyingObject flyingObject) {
        // 缩放因子，用于控制 y轴方向区域范围
        int factor = this instanceof AbstractAircraft ? 2 : 1; //我方
        int fFactor = flyingObject instanceof AbstractAircraft ? 2 : 1;//对方

        //对方坐标、宽度、高度
        int x = flyingObject.getLocationX();
        int y = flyingObject.getLocationY();
        int fWidth = flyingObject.getWidth();
        int fHeight = flyingObject.getHeight();

        return x + (fWidth+this.getWidth())/2 > locationX
                && x - (fWidth+this.getWidth())/2 < locationX
                && y + ( fHeight/fFactor+this.getHeight()/factor )/2 > locationY
                && y - ( fHeight/fFactor+this.getHeight()/factor )/2 < locationY;
    }

    public int getLocationX() {
        return locationX;
    }

    public int getLocationY() {
        return locationY;
    }

    public void setLocation(double locationX, double locationY){
        this.locationX = (int) locationX;
        this.locationY = (int) locationY;
        remainingX = 0;
        remainingY = 0;
    }

    public int getSpeedY() {
        return speedY;
    }

    public Image getImage() {
        if (image == null){
            image = ImageManager.get(this);
        }
        return image;
    }

    public int getWidth() {
        if (width == -1){
            // 若未设置，则查询图片宽度并设置
            Image resolved = ImageManager.get(this);
            if (resolved == null) {
                throw new IllegalStateException(this.getClass().getName() + " has no image!");
            }
            width = (int) resolved.getWidth();
        }
        return width;
    }

    public int getHeight() {
        if (height == -1){
            // 若未设置，则查询图片高度并设置
            Image resolved = ImageManager.get(this);
            if (resolved == null) {
                throw new IllegalStateException(this.getClass().getName() + " has no image!");
            }
            height = (int) resolved.getHeight();
        }
        return height;
    }
    public boolean notValid() {
        return !this.isValid;
    }

    /**
     * 标记消失，
     * isValid = false.
     * notValid() => true.
     */
    public void vanish() {
        isValid = false;
    }

}

