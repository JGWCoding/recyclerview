
# 根据recyclerview原理手敲recyclerview
## 架构中的核心组件：
1.回收池：能回收任意的item控件，并且返回符合类型的item控件；比如onBinderViewHolder方法中的第一个参数是从回收池中返回的</br>
2.适配器：Adapter接口，经常辅助Recyclerview实现列表展示；适配器模式，将用户界面展示与交互分离</br>
3.RecyclerView：是做触摸时间的交互，主要实现边界值判断；根据用户的触摸反馈，协调回收池对象与适配器对象之间的工作</br>
![](https://github.com/RoyXing/recyclerview/blob/master/pic/recyelrview%E6%A0%B8%E5%BF%83%E7%BB%84%E4%BB%B6.jpg)

## RecyclerView的架构在生活中的体现
传送带的工作机制:上货->传送->到达—>新增</br>
可以比作生产者与消费者</br>
充分利用传送带原理，只有用户看到的数据才会加载到内存，而看不到的在等待被加载。传送带能源源不断的传送亿级货物，Recyclerview也能够显示加载亿级item。</br>
![](https://github.com/RoyXing/recyclerview/blob/master/pic/%E4%BC%A0%E9%80%81%E5%B8%A6%E5%B7%A5%E4%BD%9C%E6%9C%BA%E5%88%B6.jpg)

## RecyclerView的加载过程描述：
第一屏：首先加载第一屏的时候 从回收池中寻找相同item的对象，发现为空，回收池把该需求转交给适配器(Adapter)然后通过适配器的onCreateViewHolder 返回一个布局，然后添加到RecyclerView的列表当中，当第一屏添加完成后，首屏加载结束</br>

滑动：把滑出屏幕的item移除放入回收池当中，同时底部需要添加新的item 先从回收池中寻找回收的有没有相同类型的item，如果有则通过onBinderViewHolder拿到布局对象 更新数据后加载到RecyclerView列表当中(新增策略)</br>

### 回收策略：
当滑动列表达到回收条件的时候把移除的view存入回收池，通过type对应当前回收的view类型 然后通过type值找到二维数组里面的索引把相应的view添加到回收池当中</br>
### 填充策略：
当需要新增item的时候 根绝getViewType 通过返回的type值 从回收池中取到对应的view填充到列表当中，同时移除回收池中的view</br>
![](https://github.com/RoyXing/recyclerview/blob/master/pic/%E5%9B%9E%E6%94%B6%E6%B1%A0%E8%AE%BE%E8%AE%A1.jpg)

## 需要重写自定义那些方法：

onMeasure</br>
onLayout</br>
onInterceptTouchEvent</br>
onTouchEvent</br>
scrollBy</br>

# RecyclerView相关知识点汇总

## 预取功能(Prefetch)
### 原理
这个功能是rv在版本25之后自带的，只要你使用的版本大于等于25，就可以使用此功能，默认为开启状态
通过LinearLayoutManager#setInitialItemPrefetchCount()控制开关<br>

android通过vsync没16ms发送一次信号来刷新UI,从而保证页面的流畅度，系统刷新UI会通过CUP产生数据，然后交给GPU来渲染页面。然而CUP处理完数据交给GPU之后就一直处于空闲状态，需要等待下一次信号来进行数据处理.<br>
rv的预取功能要做的事情就是预取接下来可能会显示的item，在下一帧到来之前提前做完数据处理，然后将获取到的数据缓存起来，待到真正要使用的时候再从缓存取出来。<br>

### 源码分析
实现预取功能的一个关键类就是GapWorker<br>
rv通过onTouchEvent中触发预取的逻辑判断，并在执行move时进行处理<br>
```Java 
case MotionEvent.ACTION_MOVE: {
   ......
        if (mGapWorker != null && (dx != 0 || dy != 0)) {
            mGapWorker.postFromTraversal(this, dx, dy);
        }
    }
} break;
```
判断依据就是通过传入的dx和dy得到手指接下来可能要移动的方向，如果dx或者dy的偏移量会导致下一个item要被显示出来则预取出来，但是是否一定能显示是不确定的。<br>
rv每次取出要显示的item其实就是取出一个viewholder，根据viewholder上关联的itemview来展示这个item，而取出viewholder最核心的方法就是：
```Java
tryGetViewHolderForPositionByDeadline(int position,boolean dryRun, long deadlineNs)
```
deadlineNs的取值一般有两种，一种兼容25之前没有预取机制的情况，兼容25之前的参数为：
```Java
static final long FOREVER_NS = Long.MAX_VALUE;
```
25之后的是实际的值，超过deadline预取则会失败，预取的本质是提高rv的整体流畅性，如果预取viewholder会造成下一帧显示卡顿就得不偿失了。<br>
预取成功的条件调用：
```Java
boolean willCreateInTime(int viewType, long approxCurrentNs, long deadlineNs) {
    long expectedDurationNs = getScrapDataForType(viewType).mCreateRunningAverageNs;
    return expectedDurationNs == 0 || (approxCurrentNs + expectedDurationNs < deadlineNs);
}
```
来进行判断，approxCurrentNs的值为
```Java
long start = getNanoTime();
if (deadlineNs != FOREVER_NS&& !mRecyclerPool.willCreateInTime(type, start, deadlineNs)) {
     // abort - we have a deadline we can't meet
    return null;
}
```
mCreateRunningAverageNs就是创建同type的holder所需要的平均时间。

## 四级缓存




