package GrupoB.Blockchain;

import GrupoB.gRPCService.ClientProto.BlockData;
import GrupoB.gRPCService.ClientProto.Blocks;

import java.util.LinkedList;
import java.util.UUID;

// Check https://learnmeabitcoin.com/guide/difficulty for hints on difficulty
// NOTE: Make difficulty final. Some X bits of difficulty, all the time.
public class Block {
    // private String prevBlockHash;
    private String blockID;
    private String merkleRoot;

    private LinkedList<String> transactions;

    public Block(String merkleRoot, LinkedList<String> transactions) {
        String uuid = UUID.randomUUID().toString().replace("-", "");

        // We only use 1/4 of the generated uuid (Like when generating nodeIDs)
        this.blockID = String.valueOf(uuid.toCharArray(), 0, uuid.length() / 4);
        this.merkleRoot = merkleRoot;
        this.transactions = transactions;
    }

    public Block(String merkleRoot, LinkedList<String> transactions, String blockID) {
        // We only use 1/4 of the generated uuid (Like when generating nodeIDs)
        this.blockID = blockID;
        this.merkleRoot = merkleRoot;
        this.transactions = transactions;
    }

    public String getBlockID() {
        return blockID;
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

        return new Block(block.getMerkleRoot(), transactions, block.getBlockID());
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

        builder.setBlockID(block.blockID);
        builder.setMerkleRoot(block.merkleRoot);
        for (int i = 0; i < block.transactions.size(); i++)
            builder.addTransaction(block.transactions.get(i));

        return builder.build();
    }
}
