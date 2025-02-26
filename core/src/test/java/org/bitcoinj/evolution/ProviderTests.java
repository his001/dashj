package org.bitcoinj.evolution;


import com.google.common.base.Preconditions;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.*;
import org.bitcoinj.params.UnitTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.*;
import org.dashj.bls.ExtendedPrivateKey;
import org.dashj.bls.JNI;
import org.dashj.bls.PrivateKey;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ProviderTests {

    Context context;
    UnitTestParams PARAMS;
    byte[] txdata;

    static String seedPhrase = "enemy check owner stumble unaware debris suffer peanut good fabric bleak outside";
    Wallet wallet;

    DeterministicKeyChain voting;
    DeterministicKeyChain owner;

    DeterministicKey ownerKeyMaster;
    DeterministicKey ownerKey;
    KeyId ownerKeyId;

    DeterministicKey votingKeyMaster;
    DeterministicKey votingKey;
    KeyId votingKeyId;

    ExtendedPrivateKey blsExtendedPrivateKey;
    BLSPublicKey operatorKey;
    BLSSecretKey operatorSecret;
    static {
        try {
            System.loadLibrary(JNI.LIBRARY_NAME);
        } catch (UnsatisfiedLinkError x) {
            fail(x.getMessage());
        }
    }

    @Before
    public void startup() throws UnreadableWalletException {
        PARAMS = UnitTestParams.get();
        context = new Context(PARAMS);
        txdata = Utils.HEX.decode("03000500010000000000000000000000000000000000000000000000000000000000000000ffffffff0502fa050105ffffffff02f4c21a3d0500000023210397181e4cc48fcba0e597bfb029d4cfc4473ae5772a0ff32223977d4e03e07fa9acf4c21a3d050000001976a91425e50daf158a83dfaacd1b77175900aa95a67d4188ac00000000260100fa050000aaaec8d6a8535a01bd844817dea1faed66f6c397b1dcaec5fe8c5af025023c35");

        DeterministicSeed seed = new DeterministicSeed(seedPhrase, null, "", 0);
        DeterministicKeyChain bip32 = new DeterministicKeyChain(seed, DeterministicKeyChain.ACCOUNT_ZERO_PATH);
        bip32.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        bip32.getKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        DeterministicKeyChain active = new DeterministicKeyChain(seed, DeterministicKeyChain.BIP44_ACCOUNT_ZERO_PATH_TESTNET);

        KeyChainGroup group = new KeyChainGroup(PARAMS);
        group.addAndActivateHDChain(bip32);
        group.addAndActivateHDChain(active);
        wallet = new Wallet(PARAMS, group);

        voting = new DeterministicKeyChain(seed, DeterministicKeyChain.PROVIDER_VOTING_PATH_TESTNET);
        owner = new DeterministicKeyChain(seed, DeterministicKeyChain.PROVIDER_OWNER_PATH_TESTNET);

        ownerKeyMaster = owner.getWatchingKey();//(ChildNumber.ZERO);
        ownerKey = HDKeyDerivation.deriveChildKey(ownerKeyMaster, ChildNumber.ZERO);
        ownerKeyId = new KeyId(ownerKey.getPubKeyHash());

        votingKeyMaster = voting.getWatchingKey();//(ChildNumber.ZERO);
        votingKey = HDKeyDerivation.deriveChildKey(votingKeyMaster, ChildNumber.ZERO);
        votingKeyId = new KeyId(votingKey.getPubKeyHash());

        blsExtendedPrivateKey = ExtendedPrivateKey.FromSeed(seed.getSeedBytes(), seed.getSeedBytes().length);
        /*operatorKey = new BLSPublicKey(blsExtendedPrivateKey.PrivateChild(new ChildNumber(9, true).getI())
                .PrivateChild(new ChildNumber(1, true).getI())
                .PrivateChild(new ChildNumber(3, true).getI())
                .PrivateChild(new ChildNumber(3, true).getI())
                .PrivateChild(0)
                .GetPublicKey());
*/
        PrivateKey operatorPrivateKey = blsExtendedPrivateKey.PrivateChild(new ChildNumber(9, true).getI())
                .PrivateChild(new ChildNumber(1, true).getI())
                .PrivateChild(new ChildNumber(3, true).getI())
                .PrivateChild(new ChildNumber(3, true).getI())
                .PrivateChild(0)
                .GetPrivateKey();
        operatorKey = new BLSPublicKey(operatorPrivateKey.GetPublicKey());
        operatorSecret = new BLSSecretKey(operatorPrivateKey);

        String inputAddress0 = "yRdHYt6nG1ooGaXK7GEbwVMteLY3m4FbVT";

        String freshAddress = wallet.freshAddress(KeyChain.KeyPurpose.CHANGE).toString();
        wallet.freshReceiveAddress();
        int tries = 0;
        while(!inputAddress0.equals(freshAddress)) {
            tries++;
            if(tries > 40)
                break;//throw new RuntimeException("can't find this address");
            freshAddress = wallet.freshAddress(KeyChain.KeyPurpose.CHANGE).toString();
            wallet.freshReceiveAddress();
        }
    }

    @Test
    public void providerRegistrationTest() throws IOException {

        byte [] hexData = Utils.HEX.decode("0300010001ca9a43051750da7c5f858008f2ff7732d15691e48eb7f845c791e5dca78bab58010000006b483045022100fe8fec0b3880bcac29614348887769b0b589908e3f5ec55a6cf478a6652e736502202f30430806a6690524e4dd599ba498e5ff100dea6a872ebb89c2fd651caa71ed012103d85b25d6886f0b3b8ce1eef63b720b518fad0b8e103eba4e85b6980bfdda2dfdffffffff018e37807e090000001976a9144ee1d4e5d61ac40a13b357ac6e368997079678c888ac00000000fd1201010000000000ca9a43051750da7c5f858008f2ff7732d15691e48eb7f845c791e5dca78bab580000000000000000000000000000ffff010205064e1f3dd03f9ec192b5f275a433bfc90f468ee1a3eb4c157b10706659e25eb362b5d902d809f9160b1688e201ee6e94b40f9b5062d7074683ef05a2d5efb7793c47059c878dfad38a30fafe61575db40f05ab0a08d55119b0aad300001976a9144fbc8fb6e11e253d77e5a9c987418e89cf4a63d288ac3477990b757387cb0406168c2720acf55f83603736a314a37d01b135b873a27b411fb37e49c1ff2b8057713939a5513e6e711a71cff2e517e6224df724ed750aef1b7f9ad9ec612b4a7250232e1e400da718a9501e1d9a5565526e4b1ff68c028763");

        Transaction providerRegistrationTransactionFromMessage = new Transaction(PARAMS, hexData);

        ProviderRegisterTx proRegTx = (ProviderRegisterTx) providerRegistrationTransactionFromMessage.getExtraPayloadObject();


        //    protx register_prepare
        //    58ab8ba7dce591c745f8b78ee49156d13277fff20880855f7cda501705439aca
        //    0
        //    1.2.5.6:19999
        //    yRxHYGLf9G4UVYdtAoB2iAzR3sxxVaZB6y
        //    97762493aef0bcba1925870abf51dc21f4bc2b8c410c79b7589590e6869a0e04
        //    yfbxyP4ctRJR1rs3A8C3PdXA4Wtcrw7zTi
        //    0
        //    ycBFJGv7V95aSs6XvMewFyp1AMngeRHBwy

        String txIdString = "e65f550356250100513aa9c260400562ac8ee1b93ae1cc1214cc9f6830227b51";
        Sha256Hash inputTransactionHashValue = Sha256Hash.wrap(Utils.HEX.decode("58ab8ba7dce591c745f8b78ee49156d13277fff20880855f7cda501705439aca"));

        String inputAddress0 = "yQxPwSSicYgXiU22k4Ysq464VxRtgbnvpJ";
        String outputAddress0 = "yTWY6DsS4HBGs2JwDtnvVcpykLkbvtjUte";
        String collateralAddress = "yeNVS6tFeQNXJVkjv6nm6gb7PtTERV5dGh";
        String collateralHash = "58ab8ba7dce591c745f8b78ee49156d13277fff20880855f7cda501705439aca";
        long collateralIndex = 0;
        TransactionOutPoint reversedCollateral = new TransactionOutPoint(PARAMS, collateralIndex, Sha256Hash.wrap(collateralHash));
        String payoutAddress = "yTb47qEBpNmgXvYYsHEN4nh8yJwa5iC4Cs";


        ECKey inputPrivateKey0 = wallet.findKeyFromPubHash(Address.fromBase58(PARAMS, inputAddress0).getHash160());

        String checkInputAddress0 = inputPrivateKey0.toAddress(PARAMS).toString();
        assertEquals("Private key does not match input address", checkInputAddress0,inputAddress0);

        ECKey collateralKey = wallet.findKeyFromPubHash(Address.fromBase58(PARAMS, collateralAddress).getHash160());
        ECKey collateralAccount = wallet.findKeyFromPubHash(Address.fromBase58(PARAMS, collateralAddress).getHash160());
        ECKey inputPrivateKey = wallet.findKeyFromPubHash(Address.fromBase58(PARAMS, inputAddress0).getHash160());

        assertEquals("Payload hash calculation has issues", proRegTx.inputsHash.toString(), "7ba273b835b1017da314a3363760835ff5ac20278c160604cb8773750b997734");

        assertEquals("Payload hash calculation has issues", proRegTx.getHash().toString(), "71e973f79003accd202b9a2ab2613ac6ced601b26684e82f561f6684fef2f102");

        assertEquals("Provider transaction collateral string doesn't match", "yTb47qEBpNmgXvYYsHEN4nh8yJwa5iC4Cs|0|yRxHYGLf9G4UVYdtAoB2iAzR3sxxVaZB6y|yfbxyP4ctRJR1rs3A8C3PdXA4Wtcrw7zTi|71e973f79003accd202b9a2ab2613ac6ced601b26684e82f561f6684fef2f102",proRegTx.makeSignString());


        String base64Signature = "H7N+ScH/K4BXcTk5pVE+bnEacc/y5RfmIk33JO11Cu8bf5rZ7GErSnJQIy4eQA2nGKlQHh2aVWVSbksf9owCh2M=";

        String signature = collateralAccount.signMessage(proRegTx.makeSignString());

        assertEquals("Signatures don't match", signature,base64Signature);
        assertEquals("Signatures don't match", Base64.toBase64String(proRegTx.signature.getBytes()), base64Signature);

        Script scriptPayout = ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, payoutAddress));

        MasternodeAddress ipAddress = new MasternodeAddress(InetAddress.getByName("1.2.5.6"), 19999);

        Script inputScript = ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, inputAddress0));

        Transaction providerRegistrationTransaction = new Transaction(PARAMS);
        providerRegistrationTransaction.setVersionAndType(3, Transaction.Type.TRANSACTION_PROVIDER_REGISTER);
        TransactionInput input = new TransactionInput(PARAMS, null, new byte[0], new TransactionOutPoint(PARAMS, 1, inputTransactionHashValue));
        TransactionOutput output = new TransactionOutput(PARAMS, null, Coin.valueOf(40777037710L), ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, outputAddress0)).getProgram());
        providerRegistrationTransaction.addOutput(output);
        ProviderRegisterTx providerRegisterTx = new ProviderRegisterTx(PARAMS, 1, 0, 0, reversedCollateral, ipAddress,
                ownerKeyId, operatorKey ,votingKeyId, 0, scriptPayout, Transaction.calculateInputsHash(input), collateralKey);
        providerRegistrationTransaction.setExtraPayload(providerRegisterTx);

        providerRegistrationTransaction.addSignedInput(input, inputScript, inputPrivateKey, Transaction.SigHash.ALL, false);


        ProviderRegisterTx fromData = (ProviderRegisterTx)providerRegistrationTransaction.getExtraPayloadObject();
        ProviderRegisterTx fromMessage = (ProviderRegisterTx)providerRegistrationTransactionFromMessage.getExtraPayloadObject();

        assertArrayEquals("Provider payload data doesn't match up", providerRegistrationTransaction.getExtraPayload(), providerRegistrationTransactionFromMessage.getExtraPayload());

        assertEquals("Provider payload collateral strings don't match up", fromData.collateralOutpoint, fromMessage.collateralOutpoint);

        assertEquals("Provider transaction port doesn't match up", fromData.address.getPort(),fromMessage.address.getPort());

        assertEquals("Provider transaction operator key is having an issue", fromData.pubkeyOperator.toString(), fromMessage.pubkeyOperator.toString());

        assertEquals("Provider transaction operator Address is having an issue", fromData.operatorReward,fromMessage.operatorReward);

        assertEquals("Provider transaction owner Address is having an issue", fromData.keyIDOwner,fromMessage.keyIDOwner);

        assertEquals("Provider transaction voting Address is having an issue", fromData.keyIDVoting,fromMessage.keyIDVoting);


        UnsafeByteArrayOutputStream bosFromData = new UnsafeByteArrayOutputStream();
        providerRegistrationTransaction.bitcoinSerialize(bosFromData);

        UnsafeByteArrayOutputStream bosFromMessage = new UnsafeByteArrayOutputStream();
        providerRegistrationTransaction.bitcoinSerialize(bosFromMessage);

        assertArrayEquals("Provider transaction does not match it's data", bosFromData.toByteArray(), hexData);
        assertArrayEquals("Provider transaction does not match it's data", bosFromMessage.toByteArray(), hexData);

        assertEquals("Provider transaction hashes aren't correct", providerRegistrationTransactionFromMessage.getHash().toString(),txIdString);

    }
    
    @Test
    public void testNoCollateralProviderRegistrationTransaction() throws IOException {

        byte [] hexData = Utils.HEX.decode("030001000379efbe95cba05893d09f4ec51a71171a3852b54aa958ae35ce43276f5f8f1002000000006a473044022015df39c80ca8595cc197a0be692e9d158dc53bdbc8c6abca0d30c086f338c037022063becdb4f891436de3d2fb21cbf294e9dcb5c1a04bc0ba621867479e46d048cc0121030de5cb8989b6902d98017ab4d42b9244912006b0a1561c1d1ba0e2f3117a39adffffffff79efbe95cba05893d09f4ec51a71171a3852b54aa958ae35ce43276f5f8f1002010000006a47304402205c1bae23b459081b060de14133a20378243bebc05c8e2ed9acdabf6717ae7f9702204027ba0abbcce9ba5b2cb563cbff0190ba8f80e5f8fd6beb07c2c449f194c9be01210270b0f0b71472736a397975a84927314261be815d423006d1bcbc00cd693c3d81ffffffff9d925d6cd8e3a408f472e872d1c2849bc664efda8c7f68f1b3a3efde221bc474010000006a47304402203fa23ec33f91efa026b34e90b15a1fd64ff03242a6a92985b16a25b590e5bae002202d1429374b60b1180cd8b9bd0b432158524f5624d6c5d2d6db8c637c9961a21e0121024c0b09e261253dc40ed572c2d63d0b6cda89154583d75a5ab5a14fba81d70089ffffffff0200e87648170000001976a9143795a62df2eb953c1d08bc996d4089ee5d67e28b88ac438ca95a020000001976a91470ed8f5b5cfd4791c15b9d8a7f829cb6a98da18c88ac00000000d101000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ffff010101014e1f3dd03f9ec192b5f275a433bfc90f468ee1a3eb4c157b10706659e25eb362b5d902d809f9160b1688e201ee6e94b40f9b5062d7074683ef05a2d5efb7793c47059c878dfad38a30fafe61575db40f05ab0a08d55119b0aad300001976a9143795a62df2eb953c1d08bc996d4089ee5d67e28b88ac14b33f2231f0df567e0dfb12899c893f5d2d05f6dcc7d9c8c27b68a71191c75400");
        String txIdString = "717d2d4a7d583da184872f4a07e35d897a1be9dd9875b4c017c81cf772e36694";
        TransactionOutPoint input0 = new TransactionOutPoint(PARAMS, 0, Sha256Hash.wrap("02108f5f6f2743ce35ae58a94ab552381a17711ac54e9fd09358a0cb95beef79"));
        TransactionOutPoint input1 = new TransactionOutPoint(PARAMS, 1, Sha256Hash.wrap("02108f5f6f2743ce35ae58a94ab552381a17711ac54e9fd09358a0cb95beef79"));
        TransactionOutPoint input2 = new TransactionOutPoint(PARAMS, 1, Sha256Hash.wrap("74c41b22deefa3b3f1687f8cdaef64c69b84c2d172e872f408a4e3d86c5d929d"));
        String inputAddress0 = "yRdHYt6nG1ooGaXK7GEbwVMteLY3m4FbVT"; //m/0'/0/1
        String inputAddress1 = "yWJqVcT5ot5GEcB8oYkHnnYcFG5pLiVVtd";
        String inputAddress2 = "ygQ8tG3tboQ7oZEhtDBBYtquTmVyiDe6d5";
        String outputAddress0 = "yRPMHZKviaWgqPaNP7XURemxtf7EyXNN1k";
        String outputAddress1 = "yWcZ7ePLX3yLkC3Aj9KaZvxRQkkZC6VPL8";
        String payoutAddress = "yRPMHZKviaWgqPaNP7XURemxtf7EyXNN1k";
        ECKey inputPrivateKey0 = wallet.findKeyFromPubHash(Address.fromBase58(PARAMS, inputAddress0).getHash160());
        ECKey inputPrivateKey1 = wallet.findKeyFromPubHash(Address.fromBase58(PARAMS, inputAddress1).getHash160());
        ECKey inputPrivateKey2 = wallet.findKeyFromPubHash(Address.fromBase58(PARAMS, inputAddress2).getHash160());

        String checkInputAddress0 = inputPrivateKey0.toAddress(PARAMS).toString();
        assertEquals("Private key does not match input address", checkInputAddress0,inputAddress0);

        String checkInputAddress1 = inputPrivateKey1.toAddress(PARAMS).toString();
        assertEquals("Private key does not match input address", checkInputAddress1,inputAddress1);

        String checkInputAddress2 = inputPrivateKey2.toAddress(PARAMS).toString();
        assertEquals("Private key does not match input address", checkInputAddress2,inputAddress2);

        Transaction providerRegistrationTransactionFromMessage = new Transaction(PARAMS, hexData);

        UnsafeByteArrayOutputStream bosFromMessage = new UnsafeByteArrayOutputStream(hexData.length);
        providerRegistrationTransactionFromMessage.bitcoinSerialize(bosFromMessage);
        assertArrayEquals("Provider transaction does not match it's data", bosFromMessage.toByteArray(),hexData);


        Script scriptPayout = ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, payoutAddress));

        MasternodeAddress ipAddress = new MasternodeAddress(InetAddress.getByName("1.1.1.1"), 19999);

        Transaction calcInputsHashTx = new Transaction(PARAMS);
        calcInputsHashTx.addInput(new TransactionInput(PARAMS, null, new byte[0], input0));
        calcInputsHashTx.addInput(new TransactionInput(PARAMS, null, new byte[0], input1));
        calcInputsHashTx.addInput(new TransactionInput(PARAMS, null, new byte[0], input2));
        Sha256Hash inputsHash = calcInputsHashTx.calculateInputsHash();

        ProviderRegisterTx proRegTx = new ProviderRegisterTx(PARAMS, 1, 0, 0, new TransactionOutPoint(PARAMS, 0, Sha256Hash.ZERO_HASH), ipAddress, ownerKeyId, operatorKey, votingKeyId, 0, scriptPayout, inputsHash);
        Transaction providerRegistrationTransaction = new Transaction(PARAMS, proRegTx);

        providerRegistrationTransaction.addOutput(new TransactionOutput(PARAMS, null, Coin.valueOf(100000000000L), Address.fromBase58(PARAMS, outputAddress0)));
        providerRegistrationTransaction.addOutput(new TransactionOutput(PARAMS, null, Coin.valueOf(10110995523L), Address.fromBase58(PARAMS, outputAddress1)));

        providerRegistrationTransaction.addSignedInput(input0, ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, inputAddress0)), inputPrivateKey0, Transaction.SigHash.ALL, false);
        providerRegistrationTransaction.addSignedInput(input1, ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, inputAddress1)), inputPrivateKey1, Transaction.SigHash.ALL, false);
        providerRegistrationTransaction.addSignedInput(input2, ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, inputAddress2)), inputPrivateKey2, Transaction.SigHash.ALL, false);


        ProviderRegisterTx fromData = (ProviderRegisterTx)providerRegistrationTransaction.getExtraPayloadObject();
        ProviderRegisterTx fromMessage = (ProviderRegisterTx)providerRegistrationTransactionFromMessage.getExtraPayloadObject();

        assertArrayEquals("Provider payload data doesn't match up", fromData.getPayload(), fromMessage.getPayload());

        UnsafeByteArrayOutputStream bosFromData = new UnsafeByteArrayOutputStream(hexData.length);
        providerRegistrationTransaction.bitcoinSerialize(bosFromData);

        //assertArrayEquals(bosFromData.toByteArray(), hexData);

        Script message = providerRegistrationTransactionFromMessage.getInput(0).getScriptSig();
        Script data = providerRegistrationTransaction.getInput(0).getScriptSig();

        assertArrayEquals(message.getPubKey(), data.getPubKey());

        message.correctlySpends(providerRegistrationTransactionFromMessage, 0, ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, inputAddress0)), Script.ALL_VERIFY_FLAGS);

        //TODO:  The signatures are not correct on the providerRegistrationTransaction
        //data.correctlySpends(providerRegistrationTransaction, 0, ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, inputAddress0)), Script.ALL_VERIFY_FLAGS);

        //assertArrayEquals("Provider transaction does not match it's data", bosFromMessage.toByteArray(), bosFromData.toByteArray());

        //assertEquals(providerRegistrationTransactionFromMessage.getHash().toString(), txIdString);

        //assertEquals(providerRegistrationTransaction.getHash().toString(), txIdString);
    }


    @Test
    public void testProviderUpdateServiceTransaction() throws IOException {

        byte [] hexData = Utils.HEX.decode("03000200018f3fe6683e36326669b6e34876fb2a2264e8327e822f6fec304b66f47d61b3e1010000006b48304502210082af6727408f0f2ec16c7da1c42ccf0a026abea6a3a422776272b03c8f4e262a022033b406e556f6de980b2d728e6812b3ae18ee1c863ae573ece1cbdf777ca3e56101210351036c1192eaf763cd8345b44137482ad24b12003f23e9022ce46752edf47e6effffffff0180220e43000000001976a914123cbc06289e768ca7d743c8174b1e6eeb610f1488ac00000000b501003a72099db84b1c1158568eec863bea1b64f90eccee3304209cebe1df5e7539fd00000000000000000000ffff342440944e1f00e6725f799ea20480f06fb105ebe27e7c4845ab84155e4c2adf2d6e5b73a998b1174f9621bbeda5009c5a6487bdf75edcf602b67fe0da15c275cc91777cb25f5fd4bb94e84fd42cb2bb547c83792e57c80d196acd47020e4054895a0640b7861b3729c41dd681d4996090d5750f65c4b649a5cd5b2bdf55c880459821e53d91c9");

        TransactionInput input0 = new TransactionInput(PARAMS, null, new byte[0], new TransactionOutPoint(PARAMS,1, Sha256Hash.wrap("e1b3617df4664b30ec6f2f827e32e864222afb7648e3b6696632363e68e63f8f")));
        String inputAddress0 = "yhmDZGmiwjCPJrTFFiBFZJw31PhvJFJAwq";
        ECKey inputPrivateKey0 = wallet.findKeyFromPubHash(Address.fromBase58(PARAMS, inputAddress0).getHash160());
        String outputAddress0 = "yMysmZV5ftuBzuvDMHWn3tMpWg7BJownRE";
        Sha256Hash providerTransactionHash = Sha256Hash.wrap("fd39755edfe1eb9c200433eecc0ef9641bea3b86ec8e5658111c4bb89d09723a");

        String checkInputAddress0 = inputPrivateKey0.toAddress(PARAMS).toString();
        assertEquals(checkInputAddress0,inputAddress0);


        Transaction providerUpdateServiceTransactionFromMessage = new Transaction(PARAMS, hexData);

        UnsafeByteArrayOutputStream fromMessage = new UnsafeByteArrayOutputStream(hexData.length);
        providerUpdateServiceTransactionFromMessage.bitcoinSerialize(fromMessage);

        assertArrayEquals(fromMessage.toByteArray(), hexData);

        BLSPublicKey operatorKeyNeeded = new BLSPublicKey(PARAMS, Utils.HEX.decode("157b10706659e25eb362b5d902d809f9160b1688e201ee6e94b40f9b5062d7074683ef05a2d5efb7793c47059c878dfa"), 0);
        assertEquals(operatorKey.toString(), operatorKeyNeeded.toString());

        Sha256Hash payloadHash = ((ProviderUpdateServiceTx)providerUpdateServiceTransactionFromMessage.getExtraPayloadObject()).getSignatureHash();

        BLSSignature signatureFromDigest = operatorSecret.Sign(payloadHash);



        ProviderUpdateServiceTx providerUpdateServiceTx = ((ProviderUpdateServiceTx)providerUpdateServiceTransactionFromMessage.getExtraPayloadObject());
        assertEquals(signatureFromDigest.toString(), providerUpdateServiceTx.signature.toString());

        Preconditions.checkState(signatureFromDigest.verifyInsecure(operatorKey, payloadHash));
        Preconditions.checkState(providerUpdateServiceTx.signature.verifyInsecure(operatorKey, payloadHash));


        MasternodeAddress ipAddress = new MasternodeAddress(InetAddress.getByName("52.36.64.148"), 19999);

        ProviderUpdateServiceTx proUpServTx = new ProviderUpdateServiceTx(PARAMS, 1, providerTransactionHash, ipAddress, new Script(new byte[0]), Transaction.calculateInputsHash(input0), operatorSecret);
        Transaction providerUpdateServiceTransaction = new Transaction(PARAMS, proUpServTx);
        providerUpdateServiceTransaction.addOutput(Coin.valueOf(1124999808L), Address.fromBase58(PARAMS, outputAddress0));
        providerUpdateServiceTransaction.addSignedInput(input0, ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, inputAddress0)), inputPrivateKey0, Transaction.SigHash.ALL, false);

        UnsafeByteArrayOutputStream fromData = new UnsafeByteArrayOutputStream(hexData.length);
        providerUpdateServiceTransaction.bitcoinSerialize(fromData);

        assertArrayEquals(fromData.toByteArray(), hexData);
    }

    @Test
    public void testProviderUpdateRegistrarTransaction() throws IOException {

        byte [] providerRegistrationTransactionData = Utils.HEX.decode("030001000183208ad8994250a0eb0ae35a2b072b65b8db87fadd2463df3464fc7341adcddc000000006b483045022100a6deda2a6dd5cafacfa893982ee3a45ec7e74a6324101af20d1fb19660f0300902205ae115609890fa2a0215f1ecbb2126b6aaf77a999c3d4bcbd08530dba716efeb012103ef6556ae33ffab22d1937e9bf03f3f5cf895e0f96005f6f0f92799a13cae2948ffffffff01ed4970d0060000001976a9145e2bc4c4222f99928f6d1957340ab090cc3a00b888ac00000000fd1201010000000000b23e38fe428255d0068cc218c2c831e3bfc87c8249f3cba941e126053fd73b8a0100000000000000000000000000ffffcff665d84e1fecb1486be55e4301c45b87cbad94daa8c5d17fdd139b654f0b1c031e1cf2b934c2d895178875cfe7c6a4f6758f02bc66eea7fc292d0040701acbe31f5e14a911cb061a2f062da2ee9f1c9682a398b97a4a31199a5aaa32ab00001976a91456bcf3cac49235537d6ce0fb3214d8850a6db77788ac43f6b45e1c6b23fa7fbeb7e21478b95718e0558b0a0d1b566d34bb85c2b397ee411f6bbb5bcc2174185a5b0529c57e346d75f7f4fbed3999ea75a1cd9af2cb4eaf7344c485c70f34c91a753e97e6454dc0bff127f79af5a6fd724195446bea181b0a");


        Transaction providerRegistrationTransactionFromMessage = new Transaction(PARAMS, providerRegistrationTransactionData);

        byte [] hexData = Utils.HEX.decode("0300030001c7de76dac8dd96f9b49b12a06fe39c8caf0cad12d23ad6026094d9b11b2b260d000000006b483045022100b31895e8cea95a965c82d842eadd6eef3c7b29e677c62a5c8e2b5dce05b4ddfc02206c7b5a9ea8b71983c3b21f4ff75ac1aa44090d28af8b2d9b93e794e6eb5835e20121032ea8be689184f329dce575776bc956cd52230f4c04755d5753d9491ea5bf8f2affffffff01c94670d0060000001976a914345f07bc7ebaf9f82f273be249b6066d2d5c236688ac00000000e4010049aa692330179f95c1342715102e37777df91cc0f3a4ae7e8f9e214ee97dbb3d0000139b654f0b1c031e1cf2b934c2d895178875cfe7c6a4f6758f02bc66eea7fc292d0040701acbe31f5e14a911cb061a2f6cc4a7bb877a80c11ae06b988d98305773f93b981976a91456bcf3cac49235537d6ce0fb3214d8850a6db77788ac2d7f857a2f15eb9340a0cfbce3ff8cf09b40e582d05b1f98c7468caa0f942bcf411ff69c9cb072660cc10048332c14c08621e7461f1f4f54b448baedc0e3434d9a7c3a1780885aaef4dd44c597b49b97595e02ad54728f572967d3ce0c2c0ceac174");
        String txIdString = "bd98378ca37d3ae6f4850b82e77be675feb3c9bc6e33cb0c23de1b38a08034c7";
        TransactionOutPoint input0 = new TransactionOutPoint(PARAMS, 0, Sha256Hash.wrap("0d262b1bb1d9946002d63ad212ad0caf8c9ce36fa0129bb4f996ddc8da76dec7"));
        String inputAddress0 = "yabJKtPXkYc8ZXQNYjdKxwG7TcpdyJN1Ns";
        ECKey inputPrivateKey = DumpedPrivateKey.fromBase58(PARAMS, "cRfAz5ZmPN9eGSkXrGk3VYjJWt8gWffLCKTy7BtAgpQZj8YPvXwU").getKey();
        String outputAddress0 = "yR6MpzaykeioS25qZWrTWx2ruHCecYjMwa";
        String votingAddress = "yWEZQmGADmdSk6xCai7TPcmiSZuY65hBmo";
        String payoutAddress = "yUE5KLX1HNA4BkjN1Zgtwq6hQ16Cvo7hrX";
        String privateOwnerKeyString = "cQpV2b9hNQd5Xs7REcrkPXmuCNDVvx6mSndr2ZgXKhfAhWDUUznB";
        String privateOperatorKey = "0fc63f4e6d7572a6c33465525b5c3323f57036873dd37c98c393267c58b50533";

        BLSSecretKey operatorKey = new BLSSecretKey(Utils.HEX.decode(privateOperatorKey));
        ECKey ownerPrivateKey = DumpedPrivateKey.fromBase58(PARAMS, privateOwnerKeyString).getKey();

        Sha256Hash providerTransactionHash = Sha256Hash.wrap("3dbb7de94e219e8f7eaea4f3c01cf97d77372e10152734c1959f17302369aa49");

        Transaction providerUpdateRegistrarTransactionFromMessage = new Transaction(PARAMS, hexData);

        UnsafeByteArrayOutputStream fromMessage = new UnsafeByteArrayOutputStream(hexData.length);
        providerUpdateRegistrarTransactionFromMessage.bitcoinSerialize(fromMessage);

        assertArrayEquals(fromMessage.toByteArray(), hexData);

        ProviderUpdateRegistarTx proUpRegTx = ((ProviderUpdateRegistarTx)providerUpdateRegistrarTransactionFromMessage.getExtraPayloadObject());
        Sha256Hash payloadHash = proUpRegTx.getSignatureHash();

        MasternodeSignature compactSignature = HashSigner.signHash(payloadHash, ownerPrivateKey);

        assertArrayEquals(compactSignature.getBytes(), proUpRegTx.signature.getBytes());

        boolean verified = HashSigner.verifyHash(payloadHash, ownerPrivateKey.getPubKeyHash(), compactSignature, new StringBuilder());

        Preconditions.checkState(verified);

        TransactionInput input = new TransactionInput(PARAMS, null, new byte[0], input0);

        Script scriptPayout = ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, payoutAddress));

        proUpRegTx = new ProviderUpdateRegistarTx(PARAMS, 1, providerTransactionHash, 0,
                operatorKey.GetPublicKey(), new KeyId(Address.fromBase58(PARAMS, votingAddress).getHash160()),
                scriptPayout, Transaction.calculateInputsHash(input), ownerPrivateKey);
        Transaction providerUpdateRegistrarTransaction = new Transaction(PARAMS, proUpRegTx);

        providerUpdateRegistrarTransaction.addOutput(Coin.valueOf(29266822857L), Address.fromBase58(PARAMS, outputAddress0));
        providerUpdateRegistrarTransaction.addSignedInput(input, ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, inputAddress0)),
                inputPrivateKey, Transaction.SigHash.ALL, false);


        assertArrayEquals(compactSignature.getBytes(), proUpRegTx.signature.getBytes());

        UnsafeByteArrayOutputStream fromData = new UnsafeByteArrayOutputStream(hexData.length);
        providerUpdateRegistrarTransaction.bitcoinSerialize(fromData);

        assertArrayEquals(fromData.toByteArray(), hexData);

        assertEquals(providerUpdateRegistrarTransaction.getHash().toString(), txIdString);

        Script data = providerUpdateRegistrarTransaction.getInput(0).getScriptSig();

        data.correctlySpends(providerUpdateRegistrarTransaction, 0, ScriptBuilder.createOutputScript(Address.fromBase58(PARAMS, inputAddress0)), Script.ALL_VERIFY_FLAGS);

    }
}
