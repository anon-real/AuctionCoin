import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ConstantsBuilder, ErgoClient, NetworkType, RestApiErgoClient}

object Main {
  val ergoClient: ErgoClient = RestApiErgoClient.create("http://95.217.135.15:9053/", NetworkType.MAINNET, "", "https://api.ergoplatform.com/api/v1/")
  val addrEnc = new ErgoAddressEncoder(NetworkType.MAINNET.networkPrefix)

  val mainContract: String = Contracts.main;
  def main(args: Array[String]): Unit = {
    // use appkit to compile the contract
    ergoClient.execute(ctx => {
      var contract = ctx.compileContract(ConstantsBuilder.create()
        .item("CONFIG_NFT", "".getBytes())
        .item("CONFIG_LP", "".getBytes())
        .build(), mainContract)
      println("Main contract address: " + addrEnc.fromProposition(contract.getErgoTree).get)

      val buyBackContract = ctx.compileContract(ConstantsBuilder.create()
        .item("CONFIG_LP", "".getBytes())
        .item("TEAM_ADDRESS", "".getBytes())
        .item("ACNFT", "".getBytes())
        .build(), Contracts.buyBack)
      println("Buy back contract address: " + addrEnc.fromProposition(buyBackContract.getErgoTree).get)

    })


  }
}