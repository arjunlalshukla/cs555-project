package client

abstract sealed class IdentityException(s: String) extends Exception(s)
final class PasswordRequiredException extends
  IdentityException("option requires password option")
final class PasswordIncompatibleException extends
  IdentityException("option is not compatible with the password option")
final class OneQueryExcpetion extends
  IdentityException("must give exactly 1 query option")
final class GetOptionException extends
  IdentityException("get option value must be 'users', 'uuids' or 'all'")
