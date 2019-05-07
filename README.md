
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
![](https://github.com/RoyXing/recyclerview/blob/master/pic/%E5%9B%9B%E7%BA%A7%E7%BC%93%E5%AD%98.png)<br>
四级缓存是rv面世之后就自带的，相比listview的二级缓存机制。rv的缓存更加的高大上<br>
rv通过Recycler来管理缓存机制，通过tryGetViewHolderForPositionByDeadline可以找到如何使用缓存。<br>
tryGetViewHolderForPositionByDeadline依次从各级缓存中去取viewHolder，如果取到直接丢给rv来展示，如果取不到最终才会执行onCreateViewHolder和onBindViewHolder方法。内部实现其实是如何从司机缓存中去取。<br>

缓存Recycler内部类中的部分成员变量
```Java
 public final class Recycler {
        final ArrayList<RecyclerView.ViewHolder> mAttachedScrap = new ArrayList();
        ArrayList<RecyclerView.ViewHolder> mChangedScrap = null;
        final ArrayList<RecyclerView.ViewHolder> mCachedViews = new ArrayList();
        private final List<RecyclerView.ViewHolder> mUnmodifiableAttachedScrap;
        private int mRequestedCacheMax;
        int mViewCacheMax;
        private RecyclerView.ViewCacheExtension mViewCacheExtension;
        RecyclerView.RecycledViewPool mRecyclerPool;
        static final int DEFAULT_CACHE_SIZE = 2;
        ..............
 }
```
其中mAttachedScrap和mChangedScrap是第一级缓存，是recycler在获取ViewHolder时最先考虑的缓存，接下来是mCachedViews，mViewCacheExtension，和mRecyclerPool分别对应2，3，4级缓存。<br>

### 各级缓存的作用
#### scrap:
scrap使用来保存被rv移除但最近又马上要使用的缓存，比如rv中item自带的动画效果。<br>
计算item的偏移量然后执行属性动画的过程，这中间可能就涉及到需要将动画之前的item保存下拉位置信息，动画后的item再保存下拉位置信息，然后利用这些位置数据生成相应的属性动画。如何保存这些ViewHolder,就需要使用到scrap了，因为这些ViewHolder数据上是没有发生变化的，只是位置发生改变。因此放到scrap中最合适。<br>

mAttachedScrap和mChangedScrap两个成员变量保存的对象是有区别的，一般调用adapter的notifyItemRangeChanged被移除的ViewHolder会保存到mChangedScrap，其余的notify系列方法(不包括notifyDataSetChanged)移除的ViewHolder会被保存到mAttachedScrap中。<br>

#### cached
就LinearLayoutManager来说cached缓存默认大小为2，它的容量非常小，作用就是rv滑动时刚被移除屏幕的ViewHolder的收容所。<br>
因为rv会认为刚被移出屏幕的ViewHolder可能接下来马上会使用到，所以不会立马设置为无效的ViewHolder，会将它们保存到cached中，但又不能将所有移出屏幕的ViewHolder都设为有效的ViewHolder，所有它的默认容量只有2个我们可以通过LinearLayoutManager#setViewCacheSize来设置这个容量大小:
```Java
public void setViewCacheSize(int viewCount) {
    mRequestedCacheMax = viewCount;
    updateViewCacheSize();
}
```
#### extension
第三级缓存,这是一个自定义的缓存，rv可以自定义缓存应为的，这里你可以决定缓存的保存逻辑，但是这个自定义缓存一般没有见过具体的使用场景，而且自定义缓存需要对源码非常熟悉，否则在rv执行item动画，或者执行notify的一系列方法后你的自定义缓存是否还能有效是一个值得考虑的问题。<br>

所以不太推荐使用该缓存，可能是google工程师自己留着自己拓展使用的，目前还只是空实现。因此其实rv所说四级缓存本质上还只是三级缓存。<br>

#### pool
唯一一个我们开发者可以方便设置的，new一个pool传进去就可以了，其它的都不需要我们来处理。这个缓存保存的对象就是那些无效的ViewHolder，虽说无效的ViewHolder上的数据无效，但是它的rootview还是可以拿来更新数据使用，这也是为什么最早的listvie有个convertView参数的原因。<br>

pool一般会和cached配合使用，cached保存不下的会被pool保存，毕竟cached的容量默认只有2，但是pool容量也是有限的，当保存满之后再有ViewHolder到来的话就只能抛弃掉。它也有一个默认的大小
```Java
private static final int DEFAULT_MAX_SCRAP = 5;
int mMaxScrap = DEFAULT_MAX_SCRAP;
```
该大小也可以调用方法来改变，一般默认即可。<br>

考虑到这么多的缓存优化才会使得rv的代码非常庞大。

## 使用RecyclerView如何提高性能
google工程师在api中提供了很多优化的api，需要我们注意合理使用
### 降低item的布局层次
这个方法适用于各种布局的编写，降低页面层次可以一定程度降低cpu渲染数据的时间成本，反应到rv中就是降低mCreateRunningAverageNs的时间，不仅目前显示的页面能加快速度，预取的成功率也会提升。因此降低item布局层次可以说是rv优化中一个对于rv源码不需要了解也能完全掌握的有效方式。

### 去除冗余的setItemClick事件
rv和listview的一个比较大的不同之处就是rv没有提供setItemClick方法，这也是自己在使用的时候不了解的一个问题。<br>

一个简单的方式直接在onBindViewHolder中设置，这种方式其实不太可取，onBindViewHolder在item进入屏幕的时候都会被调用(cached缓存着的除外)，而一般情况下都会创建一个匿名内部类来是实现，这就会导致在rv在快速滑动的时候创建很对对象，因此从这点考虑的话setItemClick应该放到其它地方。<br>

我所选取的做法就是将setItemClick事件的绑定和ViewHolder对应的rootview进行绑定，viewholder由于缓存机制的存在，它创建的个数是一定的，所以和它绑定的setItemClick对象也是一定的。<br>

还有一种做法可以通过rv自带的addOnItemTouchListener来实现点击事件，原理就是rv在触摸事件中会使用到addOnItemTouchListener中设置的对象，然后配合GestureDetectorCompat实现点击item，<br>
```Java
recyclerView.addOnItemTouchListener(this);
gestureDetectorCompat = new GestureDetectorCompat(recyclerView.getContext(), new SingleClick());

@Override
public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
    if (gestureDetectorCompat != null) {
        gestureDetectorCompat.onTouchEvent(e);
    }
    return false;
}

private class SingleClick extends GestureDetector.SimpleOnGestureListener {

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
        if (view == null) {
            return false;
        }
        final RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
        if (!(viewHolder instanceof ViewHolderForRecyclerView)) {
            return false;
        }
        final int position = getAdjustPosition(viewHolder);
        if (position == invalidPosition()) {
            return false;
        }
        /****************/
        点击事件设置可以考虑放在这里
        /****************/
        return true;
    }
}
```
相对来说是一种比较优雅的实现，但是这种实现只能设置整个item的点击，如果item内有两个view需要点击就不太适应了，具体的使用可以根据实际情况来区分。<br>

### 复用pool缓存
复用本身并不难，调用rv的setRecycledViewPool方法设置一个pool进去就可以，但是并不是说每个使用rv场景都需要一个pool，这个复用pool是针对item中包含rv的情况才适用，如果item都是普通布局就不要使用pool。<br>

如果有多个item都是嵌套rv的那么复用pool就非常有必要了，在封装adapter库时需要考虑的一个点就是如何找到item中包含的rv，可以考虑的办法就是遍历item的根部局。如果找到包含rv的，那么将对该rv设置pool，所有item中的嵌套rv都使用同一个pool即可，<br>
```Java
private List<RecyclerView> findNestedRecyclerView(View rootView) {
    List<RecyclerView> list = new ArrayList<>();
    if (rootView instanceof RecyclerView) {
        list.add((RecyclerView) rootView);
        return list;
    }
    if (!(rootView instanceof ViewGroup)) {
        return list;
    }
    final ViewGroup parent = (ViewGroup) rootView;
    final int count = parent.getChildCount();
    for (int i = 0; i < count; i++) {
        View child = parent.getChildAt(i);
        list.addAll(findNestedRecyclerView(child));
    }
    return list;
}
```
得到list之后接下来的就是给里面的rv绑定pool，可以将该pool设置为adapter库中的成员变量，每次找到嵌套的rv的item时直接将该pool设置给对应的rv即可。
关于pool源码上有一点需要在意的是，当最外层的rv滑动导致item被移除屏幕时，rv其实是通过调用removeView(view)完成的，里面的参数view就是和holder绑定的rootview。如果rootview中包含了rv，会最终调用rv的onDetachedFromWindow()；
```Java
@Override
public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
    super.onDetachedFromWindow(view, recycler);
    if (mRecycleChildrenOnDetach) {
        removeAndRecycleAllViews(recycler);
        recycler.clear();
    }
}
```
注意上面if语句，如果进入该分支里面的主要逻辑就是会清除掉scrap和cached缓存上的holder并将它们放置到pool中，默认情况下mRecycleChildrenOnDetach为false。这么设计的目的在于放置到pool中的holder要想被拿来使用还必须调用onBindViewHolder来重新进行数据绑定。默认为false，这也即使rv移除屏幕也不会使里面的holdr失效，下次再次进入屏幕就可以直接使用避免了onBindViewHolder操作。<br>

google提供了setRecycleChildrenOnDetach允许我们改变它的值，如果想要充分使用pool的功能，最好将其置为true,因为按照一般用户习惯滑出屏幕的item一般不会回滚查看，这样接下来要被滑入的item如果存在rv的情况下就可以快速复用pool中的holder，这是使用pool复用的时候一个需要注意点的地方。<br>

### 保存嵌套rv的滑动状态

这里要分两种情况，一种是移除屏幕一点后就直接重新移回屏幕，另一种是移除屏幕一段距离再移回来。<br>

这里我们会发现一个问题就是移出一点回来的rv会保留原来的滑动状态，而移出一大段距离后回来的rv会丢掉原先滑动的状态，造成这个现象的原因本质是rv的缓存机制，简单的来说就是刚滑动屏幕的会被放到cache中，而滑出一段距离的会被放到pool中，而从pool中取出的holder会重新进行数据绑定。没有保存滑动状态的话rv就会被重置掉。<br>

在LinearlayoutManager中对应的有onSaveInstanceState和onRestoreInstanceState方法来分别处理保存状态和恢复状态，它的机制其实和activity的状态恢复类似。我们需要做的是当rv被屏幕移出屏幕调用onSaveInstanceState，移回来的时候调用onRestoreInstanceState即可。<br>

需要注意的是onRestoreInstanceState需要传入一个参数parcelable，这个是onSaveInstanceState提供给我们的，parcelable里面就保存了当前的滑动位置信息，如果自己在封装adapter库的时候就需要将这个parcelable保存起来：
```Java
private Map<Integer, SparseArrayCompat<Parcelable>> states;
```

map中的key为item对应的position,考虑到















