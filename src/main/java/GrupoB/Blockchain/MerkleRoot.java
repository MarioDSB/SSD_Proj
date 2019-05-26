package GrupoB.Blockchain;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class MerkleRoot {
    private String merkleRoot;

    /**
     * Computes and returns the merkle root of a list of transactions
     * @param transactions The list of transactions
     * @return The merkle root
     */
    @SuppressWarnings("unchecked")
    public static String computeMerkleRoot(LinkedList<String> transactions) {
        LinkedList<String> currentLevel = (LinkedList<String>)transactions.clone();

        while (currentLevel.size() != 1) {
            // The list of transactions is now the new list with hashes
            // NOTE: This line of code does nothing but waste processing
            //      power in the first iteration of the while loop
            transactions = (LinkedList<String>)currentLevel.clone();

            currentLevel.clear();
            int i, threshold;

            // Check if the transactions size is odd or even
            if (transactions.size() % 2 == 0)
                threshold = transactions.size();
            else
                threshold = transactions.size() - 1;

            // Compute the hashes, grouped 2 by 2
            for (i = 0; i < threshold; i+=2) {
                String stringA = Hashing.sha1()
                        .hashString(transactions.get(i), StandardCharsets.UTF_8)
                        .toString();
                String stringB = Hashing.sha1()
                        .hashString(transactions.get(i + 1), StandardCharsets.UTF_8)
                        .toString();

                String finalHash = Hashing.sha1()
                        .hashString(stringA + stringB, StandardCharsets.UTF_8)
                        .toString();

                currentLevel.add(finalHash);
            }

            // If the transactions' size is odd, add the last hash to the list without grouping it with another
            if (transactions.size() % 2 != 0) {
                String finalHash = Hashing.sha1()
                        .hashString(transactions.get(transactions.size() - 1), StandardCharsets.UTF_8)
                        .toString();

                currentLevel.add(finalHash);
            }
        }

        return currentLevel.get(0);
    }

    public MerkleRoot(LinkedList<String> transactions) {
        this.merkleRoot = computeMerkleRoot(transactions);
    }

    public String getMerkleRoot() {
        return this.merkleRoot;
    }
}
