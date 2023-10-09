import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import org.ergoplatform.appkit.{Address, ConstantsBuilder, ErgoClient, NetworkType, RestApiErgoClient}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}
import scorex.crypto.hash.Blake2b256

object Main {
  val ergoClient: ErgoClient = RestApiErgoClient.create("http://95.217.135.15:9053/", NetworkType.MAINNET, "", "https://api.ergoplatform.com/api/v1/")
  val addrEnc = new ErgoAddressEncoder(NetworkType.MAINNET.networkPrefix)

 // for testing
  val USER_P2PK_ADDRESS: ErgoAddress = Address.create("9gN7YssQgnRzDDXHAuamAwgikRkzeLYq8Jn9YvsPcKD13ajj33u").getErgoAddress
  val USER_SECRET = "19899857734851286010002701428515536889175392016268936110507684368097105859366"


  val mainContract: String = Contracts.main;
  def main(args: Array[String]): Unit = {
    // use appkit to compile the contract
    ergoClient.execute(ctx => {

      val lpnft = ErgoId.create("915da2ac421906919351163a9afa1f17918272750987906b00247e91925e757d")
      val team = "9evruWoFfhTZ4q4Xc4whQK4bJMqMAF7z9YeygzLvCHMgR17Pt5n"
      val teamAddr = addrEnc.fromString(team).get
      val acId = ErgoId.create("02194927b8d96833067052f40f032039d55c9d9f87a2ca47b7e4d56497de0f58")

      val auctionAddr = addrEnc.fromString("quE2HvwgAp7z8q4tD1BfCfj72TPziUHqruk3L8jUWQ7bWGD7ugdkpkgYx9kztPTmJborN4baTrHuenaZrCtoB3VHeLjQUQACzZ67qpnMGUAyGs93mUpwodKekvSM8Y6rCFVMxFw76MD8ZUjMPd2J8awcfECurL9v6HkjFnzKJr72NXdQ18XYYGRneXbbkgU5qzUKTLoC2weUGsDpNhfDh9xBGV7CYconikP79c9b3ZcquSBdNCu4G5r2uvBVTHXaFgk5TioeSL4SqwS5sugeEeicTPre842uZBzNd7B3XqE75Rjum1bz1vBgGtadMqt2XRui7tqaK44LjeYfds3N4CWZsYwx69h2PpHFrU9Mzvvj3ju6XGVS5X6GnyGgttXiHgokZPhD45m2etzAUXovNPeutrw5zmQudCh9jXBSfujXDBPS7ohC7qLj3Hj4zfRJTmUzPNMEUGLED57kowMXdAq95h4LciSLcNgZjM3dAwVkkPFgTnu1gvFSR6HAb78d8cx4khpf5bvtPpRxLk1vXeqRxBagUQwgaTBJUEAbxF9bfs7TQBzboKL5krjpqXKydYQb5pbZ64bRk6U6hzNjmn8XEexRE8RACaJrk9MvtXVVqJRyKRkWRvMKgo3QtfXR1qNZcXKV5AHzwCPfHXfxXAkSkEPPXFxYyj33KLuuLAw7LyWqL1v6fFXzohq1oZB3tqXGZbksri3i86cCtMoUFUCUHT2BgCxUmQVGnuUvNTe69PU5VKraGowqUo8MJFXS7njsW7uZyK867qZpPPXwu2fLgK2whDihx6JKY1Ub3aw3mUkjbdqhMxSh4m6ZhD2piJYwtiEb7juZhrhVWVqaJDFCc2aED8Vdc1Z4W9pEt8UKhHcLcHn6CiaSHiyhFwRY3bMuVZwtY2XFwzQRSfW2VyYoC5Dqby43xYuEHe1Ld7ANMsXgHzwwkisdt4rQ1EHMynXM6926DEVYewgxrGEaXdYDymLWZ5DKHdmyvYW6keH9NysQGKytiax8wUyiqyaLhXoqbJCWxrcyu9MZLZpJFRXonKCNK1SAmRkRNQfkmY9jDVnj5nBdjtEGxa9pwAFENv6xa58J1zcA4MD6Tj9B4B36YrWT2TLs7Pp4jMJryA5sUVsL6jn7cEnbjaH78qQw2sicncneBaAoQnFJedyWfnQC3GYRAfRPp3kxfZNsJf2aQQ").get
      val auctionHash = Blake2b256.hash(auctionAddr.script.bytes)

      val buyBackContract = ctx.compileContract(ConstantsBuilder.create()
        .item("CONFIG_LP", lpnft.getBytes)
        .item("TEAM_ADDRESS", teamAddr.script.bytes)
        .item("ACNFT", acId.getBytes)
        .build(), Contracts.buyBack)
      println("Buy back contract address: " + addrEnc.fromProposition(buyBackContract.getErgoTree).get)

      val buyBackContractHash = Blake2b256.hash(buyBackContract.getErgoTree.bytes)
      var contract = ctx.compileContract(ConstantsBuilder.create()
        .item("CONFIG_LP", lpnft.getBytes)
        .item("AuctionContractHash", auctionHash)
        .item("BuyBackContractHash", buyBackContractHash)
        .build(), mainContract)
      println("Main contract address: " + addrEnc.fromProposition(contract.getErgoTree).get)

    })


  }
}