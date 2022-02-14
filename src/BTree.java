import java.util.LinkedList;
import java.util.List;

/**
 * @author tailwolf
 *
 * B树是一种绝对平衡的多路查找树，这里给出的定义如下：
 * 1）除根结点外，所有结点至少含有d棵子树，即d-1个关键字，至多含有2*d棵子树，即2*d-1个关键字（根结点关键字的个数可以小于d-1，可以没有子树，如果有子树，则至少有两棵。）
 * 2）其中，d称为b树的最小度数，且d>=2
 * 3）所有叶子结点都具有相同的深度，即都在同一层上。
 * 4）每个结点内关键字的大小是递增的。
 */
public class BTree<K extends Comparable<K>, V> {

    /**
     * 根结点
     */
    private Node<K, V> root;

    /**
     * B树结点数量
     */
    private int size;

    /**
     * B树的高度
     */
    private int height;

    /**
     * B树默认的最小度数
     */
    private static final int DEFAULT_CAPACITY = 4;

    /**
     * B树的最小度数
     */
    private final int d;


    /**
     * B树结点
     * entries，保存了关键字和对应的值。
     * childs，保存了该结点所拥有的子树
     * entryNum，结点中实际存在的关键字的个数
     */
    private class Node<K extends Comparable<K>, V> {
        Entry<K, V>[] entries;
        Node<K, V>[] childs;
        int entryNum;

        public Node(){
            this.entries = new Entry[2*d-1];
            this.childs = new Node[2*d];
        }

        public Node(Entry<K, V> entry){
            this();
            this.entries[0]  = entry;
        }

        public Node(Entry<K, V>[] entries, Node<K, V>[] childs){
            this.entries = entries;
            this.childs = childs;
        }
    }

    /**
     * 保存了关键字和其对应的值
     */
    private static class Entry<K, V> {
        public K k;
        public V v;
        public Entry(K k, V v) {
            this.k  = k;
            this.v  = v;
        }
    }

    /**
     * 查询辅助类，里面保存了查到的关键字所在的结点
     */
    private class Search<K extends Comparable<K>, V> {
        /**
         * 关键字所在的结点
         */
        Node<K, V> node;
        /**
         * 关键字在结点中的索引
         */
        int keyIndex;

        public Search(Node<K, V> node, int keyIndex) {
            this.node  = node;
            this.keyIndex  = keyIndex;
        }
    }

    public BTree(){
        this(DEFAULT_CAPACITY);
    }

    public BTree(int d){
        this.d = d;
        this.size = 0;
        this.root = new Node<>();
    }

    /**
     * b树的插入。
     * 根据定义，b树关键字数量大小为d-1 <= entryNum <= 2d-1
     * 当entryNum > 2d-1时，需要对结点进行分裂。分裂方法：
     * 从中间的位置(entryNum/2)将原结点的关键字分为两部分，右边的关键字在原结点中，左边的关键字放在新结点中，中间位置(entryNum/2)的结点插入到原结点的父结点。
     *
     * 如果上述操作导致父结点的关键字也超出了上限，则对父结点进行分裂操作，直到父结点的关键字数量小于等于2d-1。
     * @param k 要插入的关键字
     * @param v 要插入的值
     */
    public void insert(K k, V v){
        Node<K, V> u = insert(this.root, k, v, this.height);
        if(u != null){
            u.childs[1] = this.root;
            this.root = u;
            this.height++;

        }
        this.size++;
    }

    private Node<K, V> insert(Node<K, V> node, K k, V v, int h){
        int rank = rank(node.entries, k, 0, node.entryNum - 1);
        if(h == 0){
            addEntry(node, rank, new Entry<>(k, v), null, null);
            return split(node);
        }else{
            Node<K, V> u = insert(node.childs[rank], k, v, h-1);
            if(u == null){
                return null;
            }

            addEntry(node, rank, new Entry<>(u.entries[0].k, u.entries[0].v), rank, u.childs[0]);
            return split(node);
        }
    }

    /**
     * 分裂结点
     * @param node  要分裂的结点
     */
    private Node<K, V> split(Node<K, V> node) {
        if(node.entryNum < 2*d-1){
            return null;
        }

        int middle = (2*d-1) / 2;
        Entry<K, V> middleEntry = node.entries[middle];
        Node<K, V> splitNode = new Node<>(middleEntry);
        splitNode.entryNum = 1;

        Entry<K, V>[] leftEntries = new Entry[2*d-1];
        Node<K, V>[] leftChilds = new Node[2*d];
        for(int i = 0; i < middle; i++){
            leftEntries[i] = node.entries[i];
            leftChilds[i] = node.childs[i];
        }
        leftChilds[middle] = node.childs[middle];
        Node<K, V> leftNode = new Node<>(leftEntries, leftChilds);
        leftNode.entryNum = middle;
        splitNode.childs[0] = leftNode;

        for(int i = middle+1; i < node.entryNum; i++){
            node.entries[i-middle-1] = node.entries[i];
            node.childs[i-middle-1] = node.childs[i];
        }
        node.childs[node.entryNum-middle-1] = node.childs[node.entryNum];
        node.entryNum = node.entryNum - middle - 1;
        return splitNode;
    }

    /**
     * b树的删除。
     * 1.如果关键字k在当前结点中，且当前结点是叶子结点，则直接删除
     * 2.如果关键字k不在当前结点中，且当前结点不是叶子结点，则要确定包含关键字k的子树。如果子树的左兄弟或者右兄弟的关键字数量大于等于d-1，则从兄弟结点借一个关键字过来。
     *   a) 如果包含关键字k的子树的根结点的关键字数量大于d-1，则继续向下删除。
     *   b) 如果包含关键字k的子树的兄弟结点的关键字数量大于d-1，则向兄弟结点借一个关键字过来，然后继续向下删除。
     *   c) 如果包含关键字k的子树的根结点，以及该子树的兄弟结点的关键字数量，均等于d-1，需要把父结点的关键字，包含关键字子树的根结点，该子树的兄弟结点合并。然后继续向下删除。
     * 3.如果关键字k在当前结点中，则找出关键字k的直接前驱k^（直接后驱也行，这里一律用直接前驱），用k^覆盖k，然后继续向下删除k^（向下删除时，判断下一结点的关键字数量是否满足定义，不满足就继续借或者合并）
     *   直接前驱：当前关键字左侧指针所指向的子树中最右下的关键字。
     *   直接后继：当前关键字右侧指针所指向的子树中最左下的关键字。
     *
     * @param k 要删除的关键字
     */
    public V delete(K k){
        V delete = delete(this.root, k, this.height);
        //有数据返回，说明b树存在该关键字，并且进行了删除，所以size（B树结点数量）需要减一
        if(delete != null){
            this.size--;
        }
        return delete;
    }

    /**
     * 删除，递归方法
     * @param node  根结点
     * @param k     要删除的关键字
     * @param h     当前根结点的高度
     */
    private V delete(Node<K, V> node, K k, int h){
        int rank = rank(node.entries, k, 0, node.entryNum - 1);
        //当h等于0的时候，说明已经到叶子结点，直接删除
        if(h == 0){
            return delEntry(node, rank, rank);
        }

        //说明关键字在最右子树
        if(rank == node.entryNum){
            Node<K, V> leftChild = node.childs[rank - 1];
            Node<K, V> rightChild = node.childs[rank];

            if(node.childs[rank].entryNum > minEntryNum()){
                return delete(node.childs[rank], k, h-1);
            }else{
                if(leftChild.entryNum > minEntryNum()){
                    //右旋
                    rotateRight(node, rightChild, leftChild, rank-1);
                }else {
                    //合并结点
                    merge(node, rank-1, leftChild, rightChild);
                }
                return delete(node.childs[rank], k, h-1);
            }
        }
        //说明关键字在当前结点
        else if(k.compareTo(node.entries[rank].k) == 0){
            //获取直接前驱
            Node<K, V> predecessor = predecessor(node.childs[rank]);
            node.entries[rank] = predecessor.entries[predecessor.entryNum-1];

            Node<K, V> leftChild = node.childs[rank];
            Node<K, V> rightChild =  node.childs[rank+1];
            if(leftChild.entryNum > minEntryNum()){
                return delete(leftChild, node.entries[rank].k, h-1);
            }else if(rightChild.entryNum > minEntryNum()){
                //左旋
                rotateLeft(node, leftChild, rightChild, rank);
            }else {
                //合并结点
                merge(node, rank, leftChild, rightChild);
            }
            return delete(node.childs[rank], predecessor.entries[predecessor.entryNum-1].k, h-1);

        }
        //说明关键字在最左子树
        else if(rank == 0){
            Node<K, V> rightChild = node.childs[rank+1];
            Node<K, V> leftChild = node.childs[rank];

            if(node.childs[rank].entryNum > minEntryNum()){
                return delete(node.childs[rank], k, h-1);
            }else {
                if(rightChild.entryNum > minEntryNum()){
                    //左旋
                    rotateLeft(node, leftChild, rightChild, rank);
                }else {
                    //合并结点
                    merge(node, rank, leftChild, rightChild);
                }
                return delete(node.childs[rank], k, h-1);
            }
        }
        //关键字在下标为rank的子树中
        else {
            Node<K, V> leftChild = node.childs[rank-1];
            Node<K, V> rightChild = node.childs[rank+1];

            if(node.childs[rank].entryNum > minEntryNum()){
                return delete(node.childs[rank], k, h-1);
            }else{
                if(rightChild.entryNum > minEntryNum()){
                    //左旋
                    rotateLeft(node, node.childs[rank], rightChild, rank);
                }else if(leftChild.entryNum > minEntryNum()){
                    //右旋
                    rotateRight(node, node.childs[rank], leftChild, rank-1);
                }else{
                    merge(node, rank, node.childs[rank], node.childs[rank+1]);
                }
                return delete(node.childs[rank], k, h-1);
            }
        }
    }

    /**
     * 左旋
     * @param father        父结点
     * @param child         要查找的关键字所在的孩子结点
     * @param rightChild    右子结点
     * @param index         父结点当前关键字的索引
     */
    private void rotateLeft(Node<K, V> father, Node<K, V> child, Node<K, V> rightChild, int index){
        addEntry(child, child.entryNum, father.entries[index], child.entryNum+1, rightChild.childs[0]);
        father.entries[index] = rightChild.entries[0];
        delEntry(rightChild, 0, 0);
    }

    /**
     * 右旋
     * @param father        父结点
     * @param child         要查找的关键字所在的孩子结点
     * @param leftChild     左子结点
     * @param index         父结点当前关键字的索引
     */
    private void rotateRight(Node<K, V> father, Node<K, V> child, Node<K, V> leftChild, int index){
        addEntry(child, 0, father.entries[index], 0, leftChild.childs[leftChild.entryNum]);
        father.entries[index] = leftChild.entries[leftChild.entryNum-1];
        delEntry(leftChild, leftChild.entryNum-1, leftChild.entryNum);
    }

    /**
     * 返回结点最少应该含有的关键字数量
     */
    private int minEntryNum(){
        return d-1;
    }

    /**
     * 合并
     * @param fatherNode    父结点
     * @param index         索引
     * @param merge1        要合并的子结点
     * @param merge2        要合并的子结点
     */
    private void merge(Node<K, V> fatherNode, int index, Node<K, V> merge1, Node<K, V> merge2){
        Node<K, V> child = merge1.childs[merge1.entryNum];

        addEntry(merge2, 0, fatherNode.entries[index], 0, child);
        for(int i = merge1.entryNum-1; i >= 0; i--){
            addEntry(merge2, 0, merge1.entries[i], 0, merge1.childs[i]);
        }
        delEntry(fatherNode, index, index);

        if(fatherNode.entryNum == 0){
            this.height--;
            this.root = fatherNode.childs[index];
        }
    }

    /**
     * 删除关键字
     * @param   node          要删除的关键字所在的结点
     * @param   entryIndex    关键字在结点里的索引
     * @param   childsIndex   要删除的子树索引
     * @return  当删除到的时候，返回被删除关键字对应的值，没有删除到的时候就返回null
     */
    public V delEntry(Node<K, V> node, int entryIndex, int childsIndex){
        if(entryIndex == node.entryNum){
            return null;
        }

        Entry<K, V> entry = node.entries[entryIndex];
        node.entries[entryIndex] = null;
        for(int i = entryIndex; i < node.entryNum-1; i++){
            node.entries[i] = node.entries[i+1];
        }

        for(int i = childsIndex; i < node.entryNum; i++){
            node.childs[i] = node.childs[i+1];
        }
        node.entryNum--;

        return entry.v;
    }

    /**
     * 获取前驱节点
     */
    private Node<K, V> predecessor(Node<K, V> node){
        if(node.childs[0] == null){
            return node;
        }

        return predecessor(node.childs[node.entryNum]);
    }

    /**
     * 查询操作，根据关键字k查询其对应的值
     */
    public V get(K k){
        if(root.entryNum == 0){
            return null;
        }

        Search<K, V> search = get(this.root, k);
        if(search == null){
            return null;
        }

        return search.node.entries[search.keyIndex].v;
    }

    private Search<K, V> get(Node<K, V> node, K k){
        if(node == null){
            return null;
        }

        int rank = rank(node.entries, k, 0, node.entryNum - 1);
        if(rank > node.entryNum - 1){
            return get(node.childs[node.entryNum], k);
        }

        int cmp = k.compareTo(node.entries[rank].k);
        if(cmp == 0){
            return new Search<>(node, rank);
        }else if(cmp < 0){
            return get(node.childs[rank], k);
        }else{
            return get(node.childs[rank+1], k);
        }
    }

    /**
     * 更新关键字对应的值
     * @param k 关键字
     * @param v 值
     */
    public void update(K k, V v){
        Search<K, V> search = get(this.root, k);
        if(search != null){
            search.node.entries[search.keyIndex].v = v;
        }
    }

    /**
     * 二分查找，查询entries里有没有该关键字k
     */
    public int rank(Entry<K, V>[] entries, K k, int lo, int hi) {
        if (hi < lo) {
            return lo;
        }

        int mid = lo + (hi - lo) / 2;
        int cmp = k.compareTo(entries[mid].k);

        if (cmp < 0){
            return rank(entries, k, lo, mid-1);
        }
        else if (cmp > 0){
            return rank(entries, k, mid+1, hi);
        } else {
            return mid;
        }
    }

    /**
     * 遍历B树，中序遍历
     */
    @Override
    public String toString() {
        List<String> testList = new LinkedList<>();
        StringBuilder builder = new StringBuilder();
        toString(root, builder, testList);
        builder.append("\n");
        builder.append("关键字数量：" + getSize() + "\n");
        builder.append("树高：" + getHeight());
        return builder.toString();
    }

    private void toString(Node<K, V> node, StringBuilder builder, List<String> testList) {
        if(node == null){
            return ;
        }

        for(int i = 0; i < node.entryNum; i++){
            toString(node.childs[i], builder, testList);
            builder.append(node.entries[i].k + ":" + node.entries[i].v + " ");
            testList.add(node.entries[i].k+"");
        }
        toString(node.childs[node.entryNum], builder, testList);
    }


    /**
     * 向结点中增加一个关键字
     * @param node          被增加关键字的结点
     * @param entryIndex    关键字加入结点的索引
     * @param entry         要增加的关键字（entry里面保存了关键字和其对应的值）
     * @param childIndex    增加孩子的索引
     * @param child         要增加的孩子
     */
    private void addEntry(Node<K, V> node, int entryIndex, Entry<K, V> entry, Integer childIndex, Node<K, V> child){
        for(int i = node.entries.length - 2; i >= entryIndex; i--){
            node.entries[i+1] = node.entries[i];
        }
        node.entries[entryIndex] = entry;
        node.entryNum++;

        if(child != null){
            for(int i = node.childs.length - 2; i >= childIndex; i--){
                node.childs[i+1] = node.childs[i];
            }
            node.childs[childIndex] = child;
        }
    }

    /**
     * 获取该b树的关键字数量
     */
    public int getSize() {
        return size;
    }

    /**
     * 获取该关键字的高度
     */
    public int getHeight() {
        return height;
    }
}