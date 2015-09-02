package offtopic.curator

import org.apache.commons.pool2.impl.GenericObjectPool

class CuratorPool extends GenericObjectPool<CuratorClient>{
    private static CuratorPool instance = null

    public static void prepare(String zookeepers) {
        this.instance = new CuratorPool(zookeepers)
    }

    public static CuratorPool getInstance() {
        if (this.instance == null) {
            throw new Exception("Cannot access CuratorPool before prepare() has been called")
        }
        return this.instance
    }

    private CuratorPool(String zookeepers) {
        super(new CuratorClientObjectFactory(zookeepers))
    }

    public static void withCurator(Closure closure) {
        CuratorClient curator = null
        try {
            curator = CuratorPool.instance.borrowObject()
            closure.call(curator.client)
        }
        finally {
            CuratorPool.instance.returnObject(curator)
        }
    }
}
