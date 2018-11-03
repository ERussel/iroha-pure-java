package jp.co.soramitsu.iroha.java;

import com.google.protobuf.ByteString;
import iroha.protocol.BlockOuterClass;
import iroha.protocol.Commands.AddAssetQuantity;
import iroha.protocol.Commands.AddPeer;
import iroha.protocol.Commands.AppendRole;
import iroha.protocol.Commands.Command;
import iroha.protocol.Commands.CreateAccount;
import iroha.protocol.Commands.CreateAsset;
import iroha.protocol.Commands.CreateDomain;
import iroha.protocol.Commands.CreateRole;
import iroha.protocol.Commands.GrantPermission;
import iroha.protocol.Commands.SetAccountDetail;
import iroha.protocol.Commands.TransferAsset;
import iroha.protocol.Primitive.GrantablePermission;
import iroha.protocol.Primitive.Peer;
import iroha.protocol.Primitive.RolePermission;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3.CryptoException;
import jp.co.soramitsu.iroha.java.detail.BuildableAndSignable;
import jp.co.soramitsu.iroha.java.detail.mapping.AmountMapper;

public class TransactionBuilder {

  private FieldValidator validator;
  private Transaction tx = new Transaction();

  private void init(String accountId, Long time) {
    if (accountId != null) {
      setCreatorAccountId(accountId);
    }

    if (time != null) {
      setCreatedTime(time);
    }

    setQuorum(1 /* default value */);
  }

  /**
   * Both fields are required, therefore we can not create builder without them. However, in genesis
   * block they can be null.
   */
  public TransactionBuilder(String accountId, Instant time) {
    init(accountId, time.toEpochMilli());
  }

  public TransactionBuilder(String accountId, Date time) {
    init(accountId, time.getTime());
  }

  public TransactionBuilder(String accountId, Long time) {
    init(accountId, time);
  }

  public TransactionBuilder enableValidation() {
    this.validator = new FieldValidator();
    return this;
  }

  public TransactionBuilder disableValidation() {
    this.validator = null;
    return this;
  }

  public TransactionBuilder setCreatorAccountId(String accountId) {
    if (this.validator != null) {
      this.validator.checkAccountId(accountId);
    }

    tx.getProto().setCreatorAccountId(accountId);
    return this;
  }

  public TransactionBuilder setCreatedTime(Long time) {
    if (this.validator != null) {
      this.validator.checkTimestamp(time);
    }

    tx.getProto().setCreatedTime(time);
    return this;
  }

  public TransactionBuilder setCreatedTime(Date time) {
    return setCreatedTime(time.getTime());
  }

  public TransactionBuilder setCreatedTime(Instant time) {
    return setCreatedTime(time.toEpochMilli());
  }

  public TransactionBuilder setQuorum(int quorum) {
    if (this.validator != null) {
      this.validator.checkQuorum(quorum);
    }

    tx.getProto().setQuorum(quorum);
    return this;
  }

  public TransactionBuilder createAccount(
      String accountName,
      String domainid,
      byte[] publicKey
  ) {
    if (this.validator != null) {
      this.validator.checkAccount(accountName);
      this.validator.checkDomain(domainid);
      this.validator.checkPublicKey(publicKey);
    }

    tx.getProto().addCommands(
        Command.newBuilder()
            .setCreateAccount(
                CreateAccount.newBuilder()
                    .setAccountName(accountName)
                    .setDomainId(domainid)
                    .setMainPubkey(ByteString.copyFrom(publicKey)).build()
            ).build()
    );

    return this;
  }

  public TransactionBuilder createAccount(
      String accountName,
      String domainid,
      PublicKey publicKey
  ) {
    return createAccount(
        accountName,
        domainid,
        publicKey.getEncoded()
    );
  }

  public TransactionBuilder transferAsset(
      String sourceAccount,
      String destinationAccount,
      String assetId,
      String description,
      String amount
  ) {
    if (this.validator != null) {
      this.validator.checkAccountId(sourceAccount);
      this.validator.checkAccountId(destinationAccount);
      this.validator.checkAssetId(assetId);
    }

    tx.getProto().addCommands(
        Command.newBuilder()
            .setTransferAsset(
                TransferAsset.newBuilder()
                    .setSrcAccountId(sourceAccount)
                    .setDestAccountId(destinationAccount)
                    .setAssetId(assetId)
                    .setDescription(description)
                    .setAmount(AmountMapper.toProtobufValue(amount))
                    .build()
            ).build()
    );

    return this;
  }

  public TransactionBuilder transferAsset(
      String sourceAccount,
      String destinationAccount,
      String assetId,
      String description,
      BigDecimal amount
  ) {
    return transferAsset(
        sourceAccount,
        destinationAccount,
        assetId,
        description,
        amount.toPlainString()
    );
  }

  public TransactionBuilder setAccountDetail(
      String accountId,
      String key,
      String value
  ) {
    if (this.validator != null) {
      this.validator.checkAccountId(accountId);
      this.validator.checkAccountDetailsKey(key);
      this.validator.checkAccountDetailsValue(value);
    }

    tx.getProto().addCommands(
        Command.newBuilder()
            .setSetAccountDetail(
                SetAccountDetail.newBuilder()
                    .setAccountId(accountId)
                    .setKey(key)
                    .setValue(value)
                    .build()
            )
            .build()
    );

    return this;
  }

  public TransactionBuilder addPeer(
      String address,
      byte[] peerKey
  ) {
    if (this.validator != null) {
      this.validator.checkPeerAddress(address);
      this.validator.checkPublicKey(peerKey);
    }

    tx.getProto().addCommands(
        Command.newBuilder()
            .setAddPeer(
                AddPeer.newBuilder()
                    .setPeer(
                        Peer.newBuilder()
                            .setAddress(address)
                            .setPeerKey(ByteString.copyFrom(peerKey))
                    ).build()
            ).build()
    );

    return this;
  }

  public TransactionBuilder addPeer(
      String address,
      PublicKey peerKey
  ) {
    return addPeer(address, peerKey.getEncoded());
  }

  public TransactionBuilder grantPermission(
      String accountId,
      GrantablePermission permission
  ) {
    if (this.validator != null) {
      this.validator.checkAccountId(accountId);
    }

    tx.getProto().addCommands(
        Command.newBuilder()
            .setGrantPermission(
                GrantPermission.newBuilder()
                    .setAccountId(accountId)
                    .setPermission(permission)
                    .build()
            ).build()
    );

    return this;
  }

  public TransactionBuilder grantPermissions(
      String accountId,
      Iterable<GrantablePermission> permissions
  ) {
    permissions.forEach(p -> this.grantPermission(accountId, p));
    return this;
  }

  public TransactionBuilder createRole(
      String roleName,
      Iterable<? extends RolePermission> permissions
  ) {

    tx.getProto().addCommands(
        Command.newBuilder().setCreateRole(
            CreateRole.newBuilder()
                .setRoleName(roleName)
                .addAllPermissions(permissions)
                .build()
        ).build()
    );

    return this;
  }

  public TransactionBuilder createDomain(
      String domainId,
      String defaultRole
  ) {

    if (this.validator != null) {
      this.validator.checkDomain(domainId);
      this.validator.checkRoleName(defaultRole);
    }

    tx.getProto().addCommands(
        Command.newBuilder()
            .setCreateDomain(
                CreateDomain.newBuilder()
                    .setDomainId(domainId)
                    .setDefaultRole(defaultRole)
                    .build()
            )
            .build()
    );

    return this;
  }

  public TransactionBuilder appendRole(
      String accountId,
      String roleName
  ) {
    if (this.validator != null) {
      this.validator.checkAccountId(accountId);
      this.validator.checkRoleName(roleName);
    }

    tx.getProto().addCommands(
        Command.newBuilder()
            .setAppendRole(
                AppendRole.newBuilder()
                    .setAccountId(accountId)
                    .setRoleName(roleName)
                    .build()
            )
            .build()
    );

    return this;
  }

  public TransactionBuilder createAsset(
      String assetName,
      String domain,
      Integer precision
  ) {
    if (this.validator != null) {
      this.validator.checkAssetName(assetName);
      this.validator.checkDomain(domain);
      this.validator.checkPrecision(precision);
    }

    tx.getProto().addCommands(
        Command.newBuilder()
            .setCreateAsset(
                CreateAsset.newBuilder()
                    .setAssetName(assetName)
                    .setDomainId(domain)
                    .setPrecision(precision)
                    .build()
            )
            .build()
    );

    return this;
  }

  public TransactionBuilder addAssetQuantity(
      String assetId,
      BigDecimal amount
  ) {
    if (this.validator != null) {
      this.validator.checkAssetId(assetId);
      this.validator.checkAmount(amount);
    }

    tx.getProto().addCommands(
        Command.newBuilder()
            .setAddAssetQuantity(
                AddAssetQuantity.newBuilder()
                    .setAssetId(assetId)
                    .setAmount(AmountMapper.toProtobufValue(amount))
                    .build()
            )
            .build()
    );

    return this;
  }

  public BuildableAndSignable<BlockOuterClass.Transaction> sign(KeyPair keyPair)
      throws CryptoException {
    return tx.sign(keyPair);
  }

  public Transaction build() {
    tx.updatePayload();
    return tx;
  }
}
