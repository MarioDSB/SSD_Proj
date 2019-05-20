package GrupoB.Blockchain;

import GrupoB.gRPCService.ClientProto.BlockData;
import GrupoB.gRPCService.ClientProto.Blocks;

import java.util.LinkedList;

// Check https://learnmeabitcoin.com/guide/difficulty for hints on difficulty
// NOTE: Make difficulty final. Some X bits of difficulty, all the time.
public class Block {
    // private String prevBlockHash;
    // private String blockID;
    private String merkleRoot;

    private LinkedList<String> transactions;

    public Block(/*String prevBlockHash, String blockID,*/
                 String merkleRoot, LinkedList<String> transactions) {
        // this.prevBlockHash = prevBlockHash;
        // this.blockID = blockID;
        this.merkleRoot = merkleRoot;
        this.transactions = transactions;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    /**
     * Checks if the block's merkle root matches the merkle root computed by hashing the transactions
     * @return True if it matches, False otherwise
     */
    public boolean verifyBlock() {
        return merkleRoot.equals(MerkleRoot.computeMerkleRoot(this.transactions));
    }

    public static Block blockFromBlockData(BlockData block) {
        LinkedList<String> transactions = new LinkedList<>();
        for (int i = 0; i < block.getTransactionCount(); i++)
            transactions.add(block.getTransaction(i));

        return new Block(block.getMerkleRoot(), transactions);
    }

    public static LinkedList<Block> blockListFromBlocks(Blocks blocks) {
        LinkedList<Block> toReturn = new LinkedList<>();
        for (BlockData block : blocks.getBlockList()) {
            toReturn.add(blockFromBlockData(block));
        }

        return toReturn;
    }

    public static BlockData blockToBlockData(Block block) {
        BlockData.Builder builder = BlockData.newBuilder();

        builder.setMerkleRoot(block.merkleRoot);
        builder.addAllTransaction(block.transactions);

        return builder.build();
    }
}
