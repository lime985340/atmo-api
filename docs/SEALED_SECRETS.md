# Sealed Secrets (옵션B) - 핵심만

- SealedSecret은 평문 Secret을 그대로 올리는 게 아니라, `kubeseal`이 공개키로 암호화한 **암호문**을 올립니다.
- 복호화(private key)는 클러스터 내부 컨트롤러만 가능합니다.
