package trade

class TradeUnfrozen extends TradeBase {
    String  handleBatch = 'n/a'
    String  handleOperName = 'n/a'
    String  handleStatus = 'waiting'
    String  unfrozenStatus = 'starting'
    String  unfrozenType = 'normal'
    String  unfrozenParams = 'n/a'

    static constraints = {
        unfrozenType      inList: ['normal', 'royalty']
        unfrozenParams    nullable: true
    }

    static mapping = {
    }
}
